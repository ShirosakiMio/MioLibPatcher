plugins {
    java
}

group = "com.mio"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.javassist:javassist:3.29.2-GA")
    // javax.sound -> OpenAL bridge (com.mio.libpatcher.jsound).
    // compileOnly: the game itself provides lwjgl / lwjgl-openal at runtime.
    compileOnly("org.lwjgl:lwjgl:3.2.2")
    compileOnly("org.lwjgl:lwjgl-openal:3.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Premain-Class" to "com.mio.libpatcher.MainAgent",
            "Agent-Class" to "com.mio.libpatcher.MainAgent",
            "Can-Redefine-Classes" to true,
            "Can-Retransform-Classes" to true
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
