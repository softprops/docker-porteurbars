# porteurbars

> a comfortable set of handle bars for steering docker container information the right direction

A library for rendering [handlebars](http://handlebarsjs.com/) templates from [docker](https://www.docker.com/) container information.

## usage

### creating template instances

Creating a new docker template is simple. 

```scala
import scala.concurrent.ExecutionContext.Implicits.global

// create a reference to a compiled template
val template = porteurbars.Template("path/to/template")
```

By default and instance of a [tugboat](http://github.com/softprops/tugboat) `Docker` is used to communicate with Docker to resolve 
information about all currently running containers. This `Docker` instance will attempt to make some smart choices for resolving a host to bind to.
If these defaults do not work for you simply pass in your own instance of a Docker.

```scala
val template = porteurbars.Template(
  "path/to/template", docker = Docker(myCustomHost))
```

Porteurbars templates are provided container information in the same structure docker returns if you
"inspect" a container.

```bash
$ docker inspect <container_id>
```

Templates are evaluated using handlebars syntax. Handlebars is a mustache-like templating language that provides an extensible API.
This library uses [handlebars java](http://jknack.github.io/handlebars.java/) for template evaluation and rendering. If you would like to extend
templating helpers for your own needs you may do so by providing a custom compiler.

```scala
val template = porteurbars.Template(
  "path/to/template",
  compiler = porteurbars.Template.compiler.registerHelpers(
    MyCustomHelpers))
```

At this point you have a compiled template. To apply docker information, simply `apply` it.

```
val evalFuture: Future[String] = template()
evalFuture.foreach(println)
```

`evalFuture` is a Scala std library future containing the response of docker container information applied to your template.

### authoring templates

The beauty of porteurbars are that templates are __just plain handlebars templates__. Anything you can do in handlebars you can do with porteurbars.
Anything you can't do with porteurbars, you can extend it with handlebars helpers.

Out of the box, standard helpers like `each` and `if` are defined. porteurbars extends the standard set of helpers with a set useful for docker specific tasks

All handlebars templates expect a "context" referring to the data being applied to the template. porteurbars provides a top level context for "Env",
which is the current map of env vars on the host system. The root context "." refers to a list of _running_ docker containers.

```bash
$ cat ids.hbs
```

```handlebars
{{#each .}}
  {{Id}}
{{/each}}
```

The template above would print out the "Id" of each running container.

#### inspect

Sometimes you can forget what docker can tell you about running containers. Shelling out to `docker inspect <container_id>` is a common practice. As
such porteurbars defines an inspect helper that you can pass a reference to any json node provided in the context.

```bash
cat template.hbs
```

```handlebars
{{{inspect .}}}
```

The template above, roughly equivalent to the output of `docker inspect <container_id>` except it will output the json for _all_ running containers.

This helper is provided mainly for debugging purposes.


#### ect

More helpers will likely be added in the future but the goal is to define the minimal set required to make porteurbars useful to a general audience

## why not?

Inspiration for this library came out of frustration with the docker-gen cli. docker-gen designed to be a is a cli, not a library, which limits its use within other libraries.
docker-gen is more than just a template generator, its also a `watch` utility. This is a useful but coupled feature to the template generator cli. This also motivated the [docker-watch](github.com/softprops/docker-watch) library. docker-gen provides its own represtation of docker containers. If you want to access information docker exposes but which that representation doesn't not cover, you are out of luck. docker-gen's templates are authored in a `go` specfic templating language which is useful if you are used to only writing `go`. 

For these reasons porteurbars was born with the following goals in mind.

- be a library first, then a cli
- templates should be authored in a flexible language-agnotic templating language ( handlebars fits that bill well )
- only be a template generator. providing a `watch` like facitly is more like an application which composes both a watch library and a template generation library. porteurbars fills the latter role. spliting these roles let's both be incrementally improved in isolation and let's thier utility be more useful in
other contexts.

An attempt was made to fix some of these issues with docker-gen but the `go` is not my forte. Scala is fun, will work with anything also running on the JVM, and works well with my existing toolchain.

Doug Tangren (softprops) 2014
