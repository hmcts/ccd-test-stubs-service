# ccd-test-stubs-service

[![Build Status](https://travis-ci.org/hmcts/ccd-test-stubs-service.svg?branch=master)](https://travis-ci.org/hmcts/ccd-test-stubs-service)

#### Environment variables
The following environment variables are required:

| Name                          | Default  | Description               |
|-------------------------------|----------|---------------------------|
| WIREMOCK_SERVER_MAPPINGS_PATH | wiremock | Path to WireMock mappings |

__Note__: If the path to the WireMock mapping files is not set, it will use the default mappings from the project
resource repository (https://github.com/hmcts/ccd-test-stubs-service/tree/master/wiremock/mappings). If
setting the variable, please keep all WireMock json stub files in a directory named
_mappings_ and exclude this directory in the path. For e.g. if you place the _mappings_ in /home/user/mappings then
export WIREMOCK_SERVER_MAPPINGS_PATH=/home/user. If you are running data-store-api in a docker container, please make
sure the callback URLs defined in the definition file use the host as **_host.docker.internal:5555_** and if running
the data-store-api on its own (non-docker), then the host should be **_localhost:5555_**

For more information on how to define wiremock stubs, please visit http://wiremock.org/docs/stubbing.

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/ccd-test-stubs-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `5555` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:5555/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

### Idam Stub

The service can be used to stub IDAM calls. It comes with a preconfigured stub IDAM user with some defaults IDAM roles.\
When testing CCD it's very often needed to customise the roles list. For example to test how certain a feature behaves for a caseworker compared to a solicitor.\
The following endpoint can be used to change the stubbed user info at runtime by posting the desired user info as JSON:

```
/idam-user
```

Example:

```
curl -X POST \
--header 'content-type: application/json' \
--data '{"email":"auto.test.cnp@gmail.com","roles":["caseworker-autotest1","caseworker-autotest2","caseworker-autotest3","caseworker","ccd-import"],"sub":"auto.test.cnp@gmail.com","uid":"7689","name":"CCD Auto Test (Stub2)","given_name":"CCD","family_name":"Auto Test (Stub2)"}' \
http://localhost:5555/idam-user
```

The changes are not persistent, i.e. they do not survive service restarts

### PRD organisation users stub

The `/refdata/external/v1/organisations/users` stub supports selectable responses through either:

* the `stub-mode` query parameter on a direct manual call to `/refdata/external/v1/organisations/users`
* the server-side stub-state endpoint used before CCD makes its internal PRD call

Supported modes are:

* default or omitted `stub-mode`: restricted caller with organisation present
* `stub-mode=restricted-failed`: restricted caller with PRD unavailable
* `stub-mode=unrestricted-failed`: unrestricted caller with PRD unavailable
* `stub-mode=failed`: generic alias for PRD unavailable
* `stub-mode=empty`: returns `200` with an empty `users` array and no `organisationIdentifier`

The restricted versus unrestricted distinction is expected to come from the configured IDAM user roles via
`/idam-user`; the PRD stub modes let AAT select the matching PRD behaviour explicitly.

For CCD-driven calls, where CCD does not append `stub-mode`, AAT can set the server-side stub state before triggering
the CCD request. This is the intended control mechanism for CCD internal PRD calls.

The stub-state endpoint is:

* `POST /stub-state/prd-organisations-users`
* `GET /stub-state/prd-organisations-users`

`POST /stub-state/prd-organisations-users` accepts:

```json
{
  "stubMode": "present | restricted-failed | unrestricted-failed"
}
```

Example:

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  --data '{"stubMode":"restricted-failed"}' \
  http://localhost:5555/stub-state/prd-organisations-users
```

`GET /stub-state/prd-organisations-users` returns the current stored mode:

```bash
curl http://localhost:5555/stub-state/prd-organisations-users
```

Response example:

```json
{
  "stubMode": "present"
}
```

Mode behaviour when CCD calls `/refdata/external/v1/organisations/users`:

* `present`: returns `200` with `organisationIdentifier` and the configured users payload
* `restricted-failed`: returns `503` with `{"message":"PRD unavailable"}`
* `unrestricted-failed`: returns `503` with `{"message":"PRD unavailable"}`

The configured state is applied automatically when CCD calls `/refdata/external/v1/organisations/users`. The default
state is `present`. If a direct manual call explicitly includes the `stub-mode` query parameter, that explicit query
parameter takes precedence over the stored state.

The stored stub state is global to the running stub-service instance and held in memory only. It is not scenario-scoped
or user-scoped, and it does not survive service restarts. Tests that change the PRD stub state should reset it back to
`present` when they finish to avoid contaminating later tests that hit the same instance.

Examples:

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  --data '{"stubMode":"present"}' \
  http://localhost:5555/stub-state/prd-organisations-users

curl -X POST \
  -H 'Content-Type: application/json' \
  --data '{"stubMode":"restricted-failed"}' \
  http://localhost:5555/stub-state/prd-organisations-users

curl -X POST \
  -H 'Content-Type: application/json' \
  --data '{"stubMode":"unrestricted-failed"}' \
  http://localhost:5555/stub-state/prd-organisations-users
```

Examples:

```bash
curl "http://localhost:5555/refdata/external/v1/organisations/users"
curl "http://localhost:5555/refdata/external/v1/organisations/users?stub-mode=restricted-failed"
curl "http://localhost:5555/refdata/external/v1/organisations/users?stub-mode=unrestricted-failed"
curl "http://localhost:5555/refdata/external/v1/organisations/users?stub-mode=failed"
curl "http://localhost:5555/refdata/external/v1/organisations/users?stub-mode=empty"
```

### Preview deployment note

Preview deployments of `ccd-test-stubs-service` should not be assumed to provide a full public replacement for the
shared AAT stub service's WireMock admin surface. In particular, callers should not assume that the usual
`/__admin/...` WireMock endpoints will be exposed through preview ingress. Use the documented application-level stub
controls, such as `/idam-user` and `/stub-state/prd-organisations-users`, for test orchestration unless preview
environment behaviour has been verified for the specific deployment under test.

## Additional information

**_dynamic-case-data-response-transformer_**: This transformer merges the case data from request payload with stubbed
case data in the response. To use this transformer, please define it in the wiremock response mapping as configured
in _wiremock/mappings/aat_dynamic_data_about_to_submit.json_

_Example:_
If the request payload is:

`{
  "data": {
    "firstName": "Test"
  }
}`

And stubbed response payload is:

`{
  "data": {
    "lastName": "Stub"
  }
}`

Then using this transformer will produce the final response as:

`{
  "data": {
    "firstName": "Test",
    "lastName": "Stub"
  }
}`

## Example CCD definition file configuration with callbacks

An example spreadsheet definition [CCD_CNP_27_With_Callbacks.xlsx] is available under root directory with configured callback urls. This can be used / tweaked to test various CCD callback scenarios.

Urls have to go under `CaseEvent` `CaseEventToFields` tabs against various callback columns (CallBackURLAboutToStartEvent, CallBackURLAboutToSubmitEvent, CallBackURLSubmittedEvent and CallBackURLMidEvent).


## Pipeline testing in isolation

A large number of the test stubs contained in this library are callback APIs created to support specific test scenarios
built into the [CCD Data Store](https://github.com/hmcts/ccd-data-store-api)'s functional tests.  Therefor changes to
this library should be tested by re-running these tests against an instance of the Test-Stubs service containing these
changes.

As the CCD preview pipelines by default use shared instances of some services to store the case type definitions; it is
important that any test CCD pipelines are configured to run in isolation otherwise any changes to the callback URLs
risk impacting in-flight CCD preview pipelines or the custom callback URLs may be quickly overwritten by another
pipeline giving false results to your own tests.

Therefor the following steps should be applied:

* Ensure the Test-Stubs pull request (PR) that is under test has the `enable_keep_helm` label set to ensure the instance
  remains active after the pipeline is complete.
* Generate test PRs for the [CCD Definition Store](https://github.com/hmcts/ccd-definition-store-api) and
  [CCD Data Store](https://github.com/hmcts/ccd-data-store-api) and configured them to run in isolation as described in
  [Configuring pipeline isolation](https://github.com/hmcts/ccd-test-definitions/blob/master/README.md#configuring-pipeline-isolation).
* Apply the following additional changes to the CCD pipelines to make them use the Test-Stubs PR that is under test:

**[CCD Definition Store](https://github.com/hmcts/ccd-definition-store-api) additional pipeline configuration**:
* `Jenkinsfile_CNP` add the following but referring to the correct Test-Stubs PR number:
  ```
  env.TEST_STUB_SERVICE_BASE_URL = "http://ccd-test-stubs-service-pr-204-java"
  ```

**[CCD Data Store](https://github.com/hmcts/ccd-data-store-api) additional pipeline configuration**:

* `Jenkinsfile_CNP` add or override the following but referring to the correct Test-Stubs PR number:
  ```
  env.BEFTA_TEST_STUB_SERVICE_BASE_URL = "https://ccd-test-stubs-service-pr-204.preview.platform.hmcts.net"
  env.TEST_STUB_SERVICE_BASE_URL = "http://ccd-test-stubs-service-pr-204-java"
  ```
  > Note: these are two different URLs as one call is ***jenkins** FTAs* -> ***Preview** Test-Stubs* and the other is
  > ***Preview** Data-Store* -> ***Preview** Test-Stubs*.  So they both need different URLs to allow them to resolve the
  > *Test-Stubs* endpoint correctly from their respective source environment.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
