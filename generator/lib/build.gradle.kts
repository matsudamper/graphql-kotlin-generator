plugins {
    id("org.jetbrains.kotlin.jvm")
    `maven-publish`
}

group = "net.matsudamper.graphql.generator.lib"
version = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

sourceSets["main"].java.setSrcDirs(
    listOf(
        "src/main/kotlin",
    ).map { File(it) }
)

java {
    withSourcesJar()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")

    implementation("com.graphql-java:graphql-java:17.3")
    implementation("com.graphql-java:graphql-java-extended-validation:17.0")
    implementation("com.graphql-java:graphql-java-extended-scalars:17.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.matsudamper.graphql.generator"
            artifactId = "lib"
            version = project.version.toString()
            from(components["java"])
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/matsudamper/graphql-kotlin-generator")
            credentials {
                username = "matsudamper"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
