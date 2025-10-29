plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.discord4j)
    implementation(libs.annotations)
    implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "discord.arbor.security.App"
}
