# porteurbars

> a well fitting set of handlebars is important for a comfortable ride

Porteurbars is a library for rendering [handlebars](http://handlebarsjs.com/) templates with [docker](https://www.docker.com/) container information.

## usage

### creating template instances

The core interface you'll work with is a `porteurbars.Template`. Creating a new template is simple. 

```scala
import scala.concurrent.ExecutionContext.Implicits.global

// create a reference to a compiled template
val template = porteurbars.Template("path/to/template")
```

By default, and instance of a [tugboat.Docker](http://github.com/softprops/tugboat#readme) is used to communicate with Docker to resolve 
information about containers running on the current host. This `tugboat.Docker` instance will attempt to make some intelligent choices
about resolving a host to bind to.
If these defaults do not work for you, simply pass in your own instance of a Docker.

```scala
val template = porteurbars.Template(
  "path/to/template", Docker(myCustomHost))
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
val template = porteurbars.Template("path/to/template")
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

```bash
$ cat container_ids.hbs
```

```handlebars
{{#each .}}
  {{Id}}
{{/each}}
```

The template above would print out the "Id" of each running container.

#### inspect

Sometimes you may forget what's inside the container json docker provides for you. Shelling out to `$ docker inspect <container_id>` is a common practice. As
such, porteurbars defines an `inspect` handlebars helper which you can pass any reference to a json node provided in the context.

```bash
$ cat inspected_containers.hbs
```

```handlebars
{{{inspect .}}}
```

The template above, roughly equivalent to the output of running `$ docker inspect <container_id>`, except that it will output the json for _all_ running containers.

This helper is provided mainly for debugging purposes, but feel free to be creative.

#### keyvalue (kv)

Sometimes docker represents key/value pairs as a single string in the form "{key}={value}". This makes it awkward to reference one or the other from a template. For this reason, porteurbars defines a `keyvalue` helper ( and also `kv` which is its alias ) which creates a new handlebars context with a "key" and "value" context which templates have easy access to.

```bash
$ cat container_envs.hbs
```

```handlebars
{{#each .}}
  {{#each Config.Env}}
     {{#keyvalue .}}
        key is {{key}} value is {{value}}
     {{/keyvalue}}
  {{/each}}
{{/each}}
```

#### truncateId

It's a common practice for docker tooling to show the short, truncated for of identifiers. porteurbars provides a handlebars helper that does just this. 

```bash
$ cat short_ids.hbs
```

```handbars
{{#each .}}
  {{ truncateId Id }}
{{/each}}
```

#### ect

More helpers will likely be added in the future but the goal is to define the minimal set required to make porteurbars useful to a general audience

## why not _?

Inspiration for this library came out of frustration with the docker-gen cli. docker-gen was a great idea, but a few things didn't sit well with me.

docker-gen designed to be a cli, not a library, which limits its use within other libraries.
docker-gen is more than just a template generator, it's also a `watch` utility. This is an interesting but coupled feature to the template generator cli. This also motivated the [docker-watch](github.com/softprops/docker-watch) library. docker-gen provides its own representation of docker containers. If you want to access information docker exposes but which that representation doesn't not cover, you are out of luck. docker-gen's templates are authored in a `go` specific templating language which is convenient if you are used to only writing `go`. I am not.

For these reasons porteurbars was born with the following goals in mind.

- be a library first, then a cli second
- templates should be authored in a flexible language-agnotic templating language ( handlebars fits that bill well )
- focus on one thing. providing the `watch`-like facility made docker-gen feel like an application which composes both a watch library and a template generation library. porteurbars fills the latter role. splitting these roles into separate libraries let's both be incrementally improved in isolation and let's their utility be useful in other contexts.

An attempt was made to fix some of these issues with docker-gen but `go` is not my forte.
Scala is fun, will work with anything also running on the JVM, and works well with my existing toolchain.

Doug Tangren (softprops) 2014
