#!/usr/bin/env python3
"""Prueba HTTP local. Crea y elimina solamente su propio registro de demostración."""
import json
import sys
import time
import uuid
from urllib.request import Request, urlopen
from urllib.error import HTTPError

base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")

def request(method, path, data=None, expected=200):
    body = None if data is None else json.dumps(data).encode()
    req = Request(base + path, data=body, method=method, headers={"Content-Type": "application/json"})
    try:
        with urlopen(req, timeout=30) as response:
            assert response.status == expected, (response.status, expected)
            text = response.read()
            return json.loads(text) if text else None
    except HTTPError as error:
        if error.code != expected:
            raise

if __name__ == "__main__":
    data = dict(operatorName="Prueba del equipo", operatorRole="Operario", pointId="PV-001", pointName="Demo",
                assetId="ACT-001", assetCode="FRZ-001", assetType="Vitrina freezer", visitType="Mantenimiento",
                equipmentStatus="Operativo", observations="", latitude=-33.5186, longitude=-70.7159,
                locationAccuracyMeters=5, distanceToPointMeters=10, presenceVerified=True,
                timestamp=int(time.time() * 1000), clientRequestId=str(uuid.uuid4()),
                evidenceBase64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a6m0AAAAASUVORK5CYII=")
    created = request("POST", "/api/visits", data, 201)
    identifier = created["id"]
    try:
        assert request("POST", "/api/visits", data, 201)["id"] == identifier
        assert request("GET", f"/api/visits/{identifier}")["operatorName"] == data["operatorName"]
        assert any(v["id"] == identifier for v in request("GET", "/api/visits"))
        updated = request("PUT", f"/api/visits/{identifier}", {"equipmentStatus": "Con falla", "observations": "Motor detenido"})
        assert updated["observations"] == "Motor detenido"
        assert request("PATCH", f"/api/visits/{identifier}/status", {"status": "Aprobada"})["supervisorStatus"] == "Aprobada"
        request("PUT", f"/api/visits/{identifier}", {"equipmentStatus": "Con falla", "observations": "x"}, 400)
    finally:
        request("DELETE", f"/api/visits/{identifier}", expected=204)
    request("GET", f"/api/visits/{identifier}", expected=404)
    print("CRUD HTTP, idempotencia, validación 400 y ausencia 404: correctos.")
