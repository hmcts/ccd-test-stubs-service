# ccd-test-stubs-service

[![Build Status](https://travis-ci.org/hmcts/ccd-test-stubs-service.svg?branch=master)](https://travis-ci.org/hmcts/ccd-test-stubs-service)

#### Environment variables
The following environment variables are required:

| Name | Default | Description |
|------|---------|-------------|
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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

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

## Example ccd definition file configuration with callbacks

An example spreadsheet definition [CCD_CNP_27_With_Callbacks.xlsx] is available under root directory with configured callback urls. This can be used / tweaked to test various ccd callback scenarios. 

Urls have to go under `CaseEvent` `CaseEventToFields` tabs against various callback columns (CallBackURLAboutToStartEvent, CallBackURLAboutToSubmitEvent, CallBackURLSubmittedEvent and CallBackURLMidEvent).
