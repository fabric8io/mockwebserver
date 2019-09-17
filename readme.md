Mock Web Server
---------------

[![CircleCI](https://circleci.com/gh/fabric8io/mockwebserver.svg?style=svg)](https://circleci.com/gh/fabric8io/mockwebserver)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.fabric8/mockwebserver/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.fabric8/mockwebserver/)


This is a DSL wrapper around the ``okhttp`` [mockwebserver](https://github.com/square/okhttp/tree/master/mockwebserver), that intends to make it easier to use.

**Features**:

- [EasyMock](http://easymock.org)-like DSL *(expect / respond / frequency)*
- Supports most HTTP operations
- Supports Chunked responses
- Supports WebSockets
- Supports String or Object bodies (which are serialized to JSON or YAML).
- Supports SSL
- CRUD mocking

### Creating a Mock Web Server

To create an instance of the Mock Web Server:

    DefaultMockServer server = new DefaultMockServer();
    server.start();

The ``DefaultMockServer`` instance wraps around ``okhttp3.mockwebserver.MockWebServer``, and delegates lifecycle and feedback calls to it **(see below)** and on top of that provides methods for [Setting expectations][]

When the start method is invoked a random port will be bound on localhost and the Mock Web Server will listen on that port. The hostname, port or URL of the server are available:

    String hostname = server.getHostName();
    int port = server.getPort();
    String url = server.url("/api/v1/users");

#### Creating a request

To create a request using the ``okhttp`` you just need to obtain the url from Mock WebServer instance and use it in your request

    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build();
    Response response = client.newCall(request).execute();

To cleanup after using the Mock Web Server:

    server.shutdown();

### Setting expectations ###

The Mock Web Server provides a DSL for setting expectations. The entry point to the DSL is the ``expect()`` method.

For example:

    server.expect().withPath("/api/v1/users").andReturn(200, "admin").once();

With the code above, we tell the Mock Web Server that the first request send to ``/api/v1/users`` should return ``admin`` with HTTP status code 200.
Follow up request will result in Http status code 404 **(not found)**.

If instead of just once you would like the expected behavior multiple times you can use ``times(int number)``:

    server.expect().withPath("/api/v1/users").andReturn(200, "admin").times(2);

And if after the first two times you would like to change the response:

    server.expect().withPath("/api/v1/users").andReturn(200, "admin").times(2);
    server.expect().withPath("/api/v1/users").andReturn(200, "admin root").once();

To set an infinite number of times you can use ``always()``.

    server.expect().withPath("/api/v1/users").andReturn(200, "admin").always();


### Serializing response bodies ###

All the snippets above are using String responses. For complex JSON or Yaml objects handcrafting Strings can be tedious. So the DSL also supports passing objects which will be serialized to JSON / YAML.

In this example we are going to use the [User class](src/test/groovy/io/fabric8/mockwebserver/User.groovy) and pass an instance of it to the Mock Web Server:

    server.expect().get().withPath("/api/v1/users").andReturn(200, new User(1L, "admin", "true").always();
    Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build();
    Response response = client.newCall(request).execute();

In this case the response boody should looks like this:

        {
            "id": 1,
            "username": "admin",
            "enabled": true
        }

### Using WebSockets ###

To support mock of web sockets this wrapper allows you to either specify a ``request/response`` style or define a sequence of messages and the time each message will be fired.

#### Using Request Response style ####

        server.expect().get().withPath("/api/v1/users/shell")
                .andUpgradeToWebSocket()
                .open()
                    .expect("create root").andEmit("CREATED").once()
                    .expect("delete root").andEmit("DELETED").once()
                .done()
                .once()

#### Emitting timed Web Socket messages ####

        server.expect().withPath("/api/v1/users/watch")
                .andUpgradeToWebSocket()
                .open()
                    .waitFor(1000).andEmit("root - CREATED")
                    .waitFor(1500).andEmit("root - DELETED")
                .done()
                .once()

### CRUD Mocking ###

Often a rest API, will act like a CRUD (create, read, update & delete). So, it makes sense to have a different approach on setting expectations. 
Instead of setting expectations for every single request, to take advantage of the crud nature of the API. This means that a get operation will return the resource, that has been previously created.
In the same spirit a delete or update request, will act on a previously created resource and so on.

To use CRUD mocking, the user will have to implement two interfaces:

- An AttributeExtractor
- A ResponseComposer

The AttributeExtractor extracts attributes, from the path of the request and from the actual resource. Attributes is what is used to match paths to resources.
In order to have a successful match, the path attributes need to be a subset of the actual resource attributes:
The actual methods that need to be implemented are:

    AttributeSet extract(String path);
    AttributeSet extract(T object);


Each get request, may result in one or more resources (based on the attributes, as explained above). To compose multiple resources into a single response, the ResponseComposer comes into play.
The ResponseComposer is a simple object that specifies how multiple Strings can be composed into a single one:

    String compose(Collection<String> items);


#### More Examples ####

This wrapper has been extensively used at:

- [Kubernetes Client](https://github.com/fabric8io/kubernetes-client)
- [Docker Client](https://github.com/fabric8io/docker-client)
