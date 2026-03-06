# Building the schemas

This file outlines how the documents are organized in this repository and how to
build the schemas.

## Layout

Broadly speaking, the current schema sources are in `schemas/{vocabulary}`.
Older, published schemas are in `src/main/schemas/{vocabulary}`. They’re
included in some releases for convenience, but they can’t be built. (I suppose,
technically, they could be downloaded by the build process, but it seems
harmless to have copies of them here.)

Within the `schemas/{vocabulary}` hierarchy, you’ll find the actual schema
sources in `src/main/{vocabulary}`. Some schemas come in slightly different
forms, for example, you’ll find `schemas/docbook/src/main/docbook` and
`schemas/docbook/src/main/docbookxi` (for the XInclude-aware version). Where
NVDL files are provided, they’re in `src/main/nvdl`. The DocBook and Publishers
schemas are also provided as Maven-installable artifacts, the Java skeleton
to support that is in `src/main/java`.
Samples, where they’re provided, are in `src/main/samples`.
Copyright and other documentation is in `src/main/xml`.

Tests are in `src/test/{vocabulary}`. Tests are XML documents that are validated
with the appropriate RELAX NG and Schematron schemas. A small wrapper element in
each test file indicates whether validation of each type is expected to succeed
or fail.

The `tools` directory contains build tools.

## Build system

The primary build system is Gradle, but several tasks are accomplished with
XProc pipelines and XSLT stylesheets.

The most useful build targets are:

* `schemas` to build the schemas
* `schematest` to run the tests
* `release` to build the release artifact
* `mavenRelease` to build the Maven release artifact
* `clean` to delete build artifacts

To target a specific schema, run `schemas:{vocabulary}:{target}`. For example,
`schemas:publishers:schematest` will run the tests on the DocBook Publishers
schema.

The `defguide` target builds a version of the RELAX NG grammars that isn’t
appropriate for validation but contains all of the details needed to generate
documentation.
