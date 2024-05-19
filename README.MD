# Unlocking the Power of Arrow 2.0: A Comprehensive Guide

This repository contains the examples of the 2024 KotlinConf talk by Simon Vergauwen.
The project uses Ktor, Exposed and TestContainers,
to implement an end-to-end feature.

This repository contains 3 different code styles:
    1. Raise with Context Receivers (discussed in talk)
    2. Raise without Context Receivers (partially discussed)
    3. Either for comparison

The server can be run using `./gradlew run`,
and you can find 3 endpoints.

1. /premium/{username}
2. /premium2/{username}
3. /premium3/{username}

This corresponds to the implementations 1, 2, 3 listed above.

To reach the endpoints you can just use curl:

```
curl --location --request POST 'localhost:8080/premium/simon'
```

The references to the slides, and recording will be added later.
