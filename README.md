# Tinysweeper

Tinysweeper is a minimal remote attestation system that mostly relies on Platform provided attestation mechanisms with supplemental detections designed to anticipate security needs particularly in the mobile payments space.

It is an independent Android library that can be used to perform device attestation and emit an authenticated record that can be verified on a backend.

## Getting Started

### A Simple Example

In your mobile app, set up the following:

```kotlin
import com.squareup.tinysweeper.tsengine.Tinysweeper
import com.squareup.tinysweeper.tsengine.defaultRetrofit

var tsengine = Tinysweeper(ctx)

/*
 * Alternatively, you can supply your own retrofit instance or endpoint.
 * This endpoint provides a random Play Integrity nonce that's derived from your
 * parameters. It should implement the Challenge() RPC endpoint in
 * `proto/squareup/lando/challenge/challenge.proto`.
 */
// var tsengine = Tinysweeper(ctx, defaultRetrofit(endpoint))

tsengine?.start()

fun someAction() {
  val attestation = tsengine.getAttestationResult().toByteString()

  // Include/send the attestation object to your backend with requests.
}
```

This will create a serialized [Attestation object](./proto/squareup/tinysweeper/android/tinysweeper.proto#L65) for your backend to decode.

For an example of how you might decode this:

```go
import (
   "github.com/golang/protobuf"

   tpb "github.com/square/tinysweeper-android/proto/squareup/tinysweeper/android"
)

func Attest(serialized bytes[]) {
   a := &tpb.Attestation{}
   if err := proto.Unmarshal(serialized, a); err != nil {
      ...
   }
   // Play Integrity is a separate first-class detection.
   // Verify it separately by following https://developer.android.com/google/play/integrity/verdicts
   if ok := checkPlayIntegrity(a.GetPlayIntegrityAttestation()); ok {
      // ...
   }
   for _, d := a.GetDetections() {
      if d.GetTriggeredAtS() == d.GetLastEvaluationS() {
         // Detection was triggered!
         // ...
      }
   }
}
```

# Development

Import the project into Android Studio to get started. Run the tests from the command line with:

```
$ cd android
$ ./gradlew test
```

## Writing your own Detection

See the [Detector abstract class](./android/tsengine/src/main/java/com/tinysweeper/tsengine/Detector.kt) or an
[example Detection](./android/tsengine/src/main/java/com/tinysweeper/tsengine/DeveloperModeDetection.kt).
