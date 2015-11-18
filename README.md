# Otto: Models and Serialization

<img src="/doc/otto.jpg" alt="Otto" title="Otto" />

Construct models for your domain as Clojure maps, with a declarative data syntax for specifying serialization and deserialization to Clojure maps compatible with other representations.

Prismatic Schema is currently used for the schema validation facilities, whereas simple Clojure functions and multimethods are used to manipulate the domain model map.

[![Build Status](https://travis-ci.org/semperos/otto.svg?branch=master)](https://travis-ci.org/semperos/otto)

## Usage

An example domain model representing a student:

```clj
(def Student
  {:id {:in s/Uuid
        :out UuidStr}
   :school-id {:key {:out "schoolId"}
               :in s/Int}
   :email {:in (s/maybe NonEmptyString)
           :out (s/maybe s/Str)}
   :first-name {:key {:out "firstName"}
                :in NonEmptyString
                :json s/Str}
   :last-name {:key {:out "lastName"}
               :in (s/maybe NonEmptyString)
               :out s/Str}
   :image {:in NonEmptyString}
   :created-at {:optional? true
                :json :otto/omit
                :in (s/maybe Timestamp)}
   :updated-at {:json :otto/omit
                :optional? true
                :in (s/maybe Timestamp)}})
```

This represents the "ideal" student domain model. Keys are expected to be keywords. Values are maps that may include the following entries:

 * `:in`: This is the schema for the value of the domain model entry
 * `:out`: This is the schema for the value of the default serialized representation of this domain model. Users may use arbitrary keywords to specify specific representations, e.g., `:json`.
 * `:key`: This is a map of data about the key itself, which may specify `:optional? true` if the key is optional in the domain model, as well as `:out` or specific representation entries that provide static values for keys (e.g., `:json "firstName"` where your key is `:first-name`).

### Schemas ###

One may extract the schema for the domain model itself using using `domain-schema`:

```clj
(require '[semperos.otto :as o])

(o/domain-schema Student)
;=> <Prismatic schema for Student domain model>
```

One may extract the schema for one of the supported output formats using `representation-schema`:

```clj
(require '[semperos.otto :as o])

(o/representation-schema :json Student)
;=> Prismatic schema for JSON-compatible Clojure map representation of domain model>
```

For representations, the `:out` key is used as a fallback when the specified representation is not included in the domain model specification.

### Serialization/Deserialization ###

All serialization/deserialization in Otto is from **Clojure maps to Clojure maps**. The translation is from Clojure maps that adhere to the schema of the domain model (presumably with all the conveniences of Clojure/JVM data structures) to Clojure maps that adhere to the schema of a particular representation (e.g., a Clojure map destined to be turned into a string of legal JSON) and back again.

This library provides **no** facilities for generating proper JSON or any other data format. It focuses simply on providing a declarative way to talk about how your domain model is structured and how it needs to be transformed before being rendered into a particular representation.

The `:in` and `:out` keys dictate what happens on serialization as opposed to deserialization. The `:in` direction is for things that are not in the format of the domain model being deserialized into its format, whereas `:out` or specific representations like `:json` are used to indicate how Clojure domain model values should be transformed on their way out.

For this purpose, there is a `semperos.otto.coerce/coerce` multimethod that dispatches on a 2-item vector, representing `[from-value to-value]` relationships. By default, Otto ships with some sane defaults for going to and from UUID objects and UUID strings, numbers, etc.

End users should **define methods for `coerce`** to support the custom schema types that they use in their domain models.

```clj
(require '[semperos.otto :as o])

(def student-map {:id #uuid "43d97569-fdc5-4786-89b6-7d0c8d81e846"
                  :school-id 42
                  :email "test@test.org"
                  :first-name "Test"
                  :last-name "Tester"
                  :image "test.png"})

(def student-json-map (o/serialize :json Student student-map))

(= student-map (deserialize Student student-json-map))
```

By default, deserialization will look for stringified versions of the Clojure domain model keys if no `:out` or specific representation is provided. If a key should be ommitted, use `:otto/omit` as the value for a representation in the domain model spec.

## License

Copyright © 2015 RentPath, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
