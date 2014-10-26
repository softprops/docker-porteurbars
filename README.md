# porteurbars

> a well fitting set of handlebars is important for a comfortable ride

Porteurbars is a library for rendering [handlebars](http://handlebarsjs.com/) templates with [docker](https://www.docker.com/) container information.

## usage

### creating template instances

The core interface you'll work with is a `porteurbars.Template`. Creating a new template is simple. 

```scala
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import porteurbars.Template

// create a reference to a compiled template
val template = Template(new File("path/to/template.hbs"))
```

You can also create a Template instance directly from the string source, an input stream or a [URL](http://docs.oracle.com/javase/7/docs/api/java/net/URL.html)

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import porteurbars.Template
val sourced = Template("{{{inspect .}}}")
val streamed = Template(inputStream)
val urld = Template(myUrl)
```

By default, and instance of a [tugboat.Docker](http://github.com/softprops/tugboat#readme) is used to communicate with Docker to resolve 
information about containers running on the current host. This `tugboat.Docker` instance will attempt to make some intelligent choices
about resolving a host to bind to.
If these defaults do not work for you, simply pass in your own instance of a Docker.

```scala
import porteurbars.Template
import java.io.File
import tugboat.Docker

val template = Template(
  new File("path/to/template.hbs"),
  Docker(myCustomHost))
```

Porteurbars templates are provided with container information in the same json structure docker returns if you
"inspect" a container from the command line.

```bash
$ docker inspect <container_id>
```

Templates are parsed and evaluated using handlebars syntax. Handlebars is a [mustache](http://mustache.github.io/)-like templating language that provides an extensible API.
This library uses [handlebars java](http://jknack.github.io/handlebars.java/) for template evaluation and rendering. If you would like to extend the set of built-in templating helpers for your own needs, you may do so using the `configure` method which takes the an instance of the current Handlebars compiler and returns a new Handlebars compiler.

```scala
import porteurbars.Template
import java.io.File
val template = Template(new File("path/to/template.hbs"))
  .configure(_.registerHelpers(MyCustomHelpers))
```

At this point you have a compiled template. To apply docker information, simply call it's `apply` method.

```scala
val evalFuture: Future[String] = template()
evalFuture.foreach(println)
```

The result of an applied template is a Scala standard library [Future](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) containing the response of docker container information applied to your handlebars template.

### authoring templates

The beauty of porteurbars is that porteurbars templates are __just plain handlebars templates__. Anything you can do in handlebars you can do with porteurbars.
Anything you can't do with porteurbars, you can easily add by extending it with handlebars helpers.

Out of the box, standard handlebars helpers like `each` and `if` are defined. porteurbars extends the standard set of helpers with a set useful for docker specific tasks.

#### template context

All handlebars templates expect a "context", which refers to the data being applied to the template. porteurbars provides a top level context name "Env",
which holds the current map of env vars on the host system. The root context "." refers to a list of _running_ docker containers.

```handlebars
{{#each .}}
  {{Id}}
{{/each}}
```

The template above would print out the "Id" of each running container.


Template instances may also be filtered by a set of preconditions. using the `filter` method. This method accepts a function which taks the json from a containers inspection and returns true if the container should be include in the template context or false.

```scala
template.filter(json => only(json))
```

For convenience template instances define two precomposed template filters `Template.exposed` which will include only those containers with exposed ports, and `Template.published` which will include only those containers with exposed ports published to the host.

#### inspect

Sometimes you may forget what's inside the container json docker provides for you. Shelling out to `$ docker inspect <container_id>` is a common practice. As
such, porteurbars defines an `inspect` handlebars helper which you can pass any reference to a json node provided in the context.

```handlebars
{{{inspect .}}}
```

The template above, roughly equivalent to the output of running `$ docker inspect <container_id>`, except that it will output the json for _all_ running containers.

This helper is provided mainly for debugging purposes, but feel free to be creative.

#### keyvalue (kv)

Sometimes docker represents key/value pairs as a single string in the form "{key}={value}". This makes it awkward to reference one or the other from a template. For this reason, porteurbars defines a `keyvalue` helper ( and also `kv` which is its alias ) which creates a new handlebars context, preserving the original keyval string combined with a "@key" and "@value" attribute which templates have easy access to.

```handlebars
{{#each .}}
  {{#each Config.Env}}
     {{#keyvalue .}}
        key is {{@key}} value is {{@value}}
     {{/keyvalue}}
  {{/each}}
{{/each}}
```

#### imagespec

Docker image names are typically composed of three parts, two of which are optional in the form below.

```
[registry/]repository[:tag]
```

To make it simpler to reference the individual parts of this image spec, porteurbars provides a `imagespec` helper
that parses in image name into a handlebars context which exposes the specific parts as meta attributes `@registry`, `@repo`, and
`@tag`.

The following template should print out these components of each of the running containers

```handlebars
{{#each .}}
  {{imagespec Config.Image}}
    registry {{ @registry }} repository {{ @repo }} tag {{ @tag }}
  {{/imagespec}}
{{/each}}
```

#### portspec

Docker ports can optionally define a type, typically `tcp` or `udp`, by annotating an integer with `/{type}`. Porteurbars defines a `portspec` helper
that parses this information out of ports into a handlebars context which exposes meta attributes `@port` and `@type`.

```handlebars
{{#each .}} {{#each NetworkSettings.Ports }}
  container {{ ../Id }} - {{#portspec @key }} port {{ @port }} type {{ @type }} {{/portspec}}
{{/each}} {{/each}}
```

#### truncateId

It's a common practice for docker tooling to show the short, truncated form of container identifiers. Porteurbars provides a handlebars helper that does just this.

```handlebars
{{#each .}}
  {{ truncateId Id }}
{{/each}}
```

#### ect

More helpers will likely be added in the future but the goal is to define the minimal set required to make porteurbars useful to a general audience

## why not _?

Inspiration for this library came out of frustration with the docker-gen cli. docker-gen was a great idea, but a few things didn't sit well with me.

docker-gen designed to be a cli, not a library, which limits its use within other libraries.
docker-gen is more than just a template generator, it's also a `watch` utility. This is an interesting but coupled feature to the template generator cli. This also motivated the [docker-watch](http://github.com/softprops/docker-watch) library. docker-gen provides its own representation of docker containers. If you want to access information docker exposes but which that representation doesn't not cover, you are out of luck. docker-gen's templates are authored in a `go` specific templating language which is convenient if you are used to only writing `go`. I am not.

For these reasons porteurbars was born with the following goals in mind.

- be a library first, then a cli second
- templates should be authored in a flexible language-agnotic templating language ( handlebars fits that bill well )
- focus on one thing. providing the `watch`-like facility made docker-gen feel like an application which composes both a watch library and a template generation library. porteurbars fills the latter role. splitting these roles into separate libraries let's both be incrementally improved in isolation and let's their utility be useful in other contexts.

An attempt was made to fix some of these issues with docker-gen but `go` is not my forte.
Scala is fun, will work with anything also running on the JVM, and works well with my existing toolchain.

Doug Tangren (softprops) 2014
