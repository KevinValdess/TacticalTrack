import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    jacoco
}

// Firebase se activa al añadir la configuración del proyecto.
if(file("google-services.json").exists()) apply(plugin="com.google.gms.google-services")

val releaseKeys = Properties().apply {
    val source = rootProject.file("keystore.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}
val backendUrl = providers.gradleProperty("backendUrl").orElse("http://10.0.2.2:8080/").get()
require(backendUrl.endsWith("/")) { "backendUrl debe terminar en /" }
require(backendUrl.matches(Regex("https?://[a-zA-Z0-9.\\-:/]+/"))) { "backendUrl inválida" }


android {
    namespace = "cl.duoc.tacticaltrack"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "cl.duoc.tacticaltrack"
        minSdk = 26
        targetSdk = 35
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseKeys.isNotEmpty()) {
            create("evaluation") {
                storeFile = rootProject.file(releaseKeys.getProperty("storeFile"))
                storePassword = releaseKeys.getProperty("storePassword")
                keyAlias = releaseKeys.getProperty("keyAlias")
                keyPassword = releaseKeys.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug { enableUnitTestCoverage = true }
        release {
            if (releaseKeys.isNotEmpty()) signingConfig = signingConfigs.getByName("evaluation")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val mockkInstrumentationAgent by configurations.creating

dependencies {
    mockkInstrumentationAgent("net.bytebuddy:byte-buddy-agent:1.14.17") { isTransitive = false }
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


// Cobertura de ViewModels, repositorios y reglas; se excluyen UI y adaptadores.
jacoco { toolVersion = "0.8.12" }
val logicClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
    include("**/viewmodel/VisitViewModel*.class", "**/viewmodel/LoginViewModel*.class", "**/viewmodel/WeatherViewModel*.class")
    include("**/data/repository/AuthRepository*.class", "**/data/repository/VisitRepository*.class", "**/data/repository/WeatherRepository*.class", "**/data/repository/CatalogRepository*.class", "**/data/repository/DeviceRegistrationRepository*.class", "**/domain/*.class")
    exclude("**/*Factory*.class")
}
val logicSources = files("src/main/java")
val coverageData = files(
    layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"),
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
).filter { it.exists() }
tasks.register<JacocoReport>("logicCoverageReport") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(logicClasses)
    sourceDirectories.setFrom(logicSources)
    executionData.setFrom(coverageData)
    reports { xml.required.set(true); html.required.set(true) }
}
tasks.register<JacocoCoverageVerification>("verifyLogicCoverage") {
    dependsOn("logicCoverageReport")
    classDirectories.setFrom(logicClasses)
    sourceDirectories.setFrom(logicSources)
    executionData.setFrom(coverageData)
    violationRules { rule { limit { counter = "LINE"; minimum = "0.80".toBigDecimal() } } }
    doFirst { check(coverageData.files.any { it.length() > 0 }) { "No se generaron datos de cobertura" } }
}
tasks.matching { it.name == "validateSigningRelease" }.configureEach {
    doFirst { check(releaseKeys.isNotEmpty()) { "Configura keystore.properties antes de generar release" } }
}

// Cargar instrumentación al iniciar la JVM evita depender de auto-attach del sistema.
tasks.withType<Test>().configureEach {
    jvmArgs("-javaagent:${mockkInstrumentationAgent.singleFile}")
}
