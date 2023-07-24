# Quarkus Scripting

A quarkus extension that simplifies running quarkus scripts.

## Usage

Add the quarkus-scripting extension to your project:
```kotlin
dependencies {
    implementation("org.chop:quarkus-scripting:1.0.0-SNAPSHOT")
}
```

Then to define a script, add a script bean to your application:
```kotlin
@ApplicationScoped
@Unremovable
class HelloWorldScript : Script() {
    override fun run() {
        Log.info("Hello World!")
    }

    override fun getName(): String {
        return "hello"
    }
}
```

This will expose the script on the http endpoint `POST /scripts/hello`.

## Running a script.

To run the script you will need to run your quarkus application in dev mode.

There are 2 ways to run the script
1. You can call the http endpoint `POST /scripts/{script-name}`.
2. Or you can use the quarkus dev mode interactive terminal. When starting dev mode, type `:`, then you can run the
   script by running the command `run {script-name}`.
