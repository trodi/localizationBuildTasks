# localization-build-tasks

A tool for preparing resources for translation and integrating them back into your product.

## Problem

In a large code base, I've found that delivering resources to translators and integrating their work back into a product to be a pain point even though all my resources are in 
properties files as they should be. You can give them your entire directory structure of resource files which may work the first time you translate your product, but I don't
like the idea of relying on a third party to find and update translations the next next go around and I don't want to pay for them to re-translate an entire product. I needed a way 
to:
* compare the current state of resources with the state of the last translations
* output what key/value pairs had been added/deleted/updated
* import the updated translations into my localized resource files
* integrate this into the build process

## How to Use

1. 	Call from a build task just like any custom java ant build task.

## How to Develop

1.	Install Java 1.7 and set up JAVA_HOME
2.	Install Scala 2.10.2 and set up SCALA_HOME
3.	Install Ant, build with "compile" task
4.	I imported into Scala IDE, use similar style

## Dev Tasks

* [x] create simple build system
  *  first pass was Ant as I'm familiar with it (sbt or gradle might be good choices, though this is an incredibly simple build)
* [x] read in resources (support for .resx and .properties)
* [x] compare current state with previous translated state
* [x] output key/value had been added/deleted/updated
* [ ] add unit tests (first pass skipped as I hadn't decided which framework was best for scala)
  * probably should use ScalaTest
* [ ] make output a simple text file
* [ ] insert new translated files back into products properties files (needs to be called as a separate build task)
* [ ] option to output all properties for when you are adding a new language

## License

[MIT license](LICENSE.md)