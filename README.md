*mpjsons* is a fast and very easy to use *object* to *json* conversion library for Scala.

It's main focus is to deliver simplest possible API, that will allow developers to serialize their objects to json without need to write custom serializers and deserialize json to final object represetation with the same principle.

Code sample:

```scala
// create mpjsons instance
val mpjsons = new MPJsons
// declare type you will be using
case class User(firstName: String, lastName: String)
// create instance of given type
val john = User("John", "Doe")
// create string with json representation of your object
val serializedJohn: String = mpjsons.serialize[User](john)
// print it to standard out
println(serializedJohn)
// This is the result
{"lastName":"Doe","firstName":"John"}
```

And code sample for deserialization:

```scala
// we are using same mpjsons instance and User type as above

// get serialized data
val serializedBob = """{"firstName":"Bob", "lastName":"Smith"}"""
// deserialize it to User type
val bob = mpjsons.deserialize[User](serializedBob)
// print it to standard out
println(bob)
// This is the result
User(Bob,Smith)
```

All the magic happens in those two simple commands `mpjsons.serialize[User](john)` and  `mpjsons.deserialize[User](serializedBob)`. For most use cases this is all you will need.

**Features**

 - Beans serialization based on their fields
 - Support for most of the standard Scala collections, `Map`, `Set`, `List`, `Vector`, etc... `mutable` and `immutable`, also `Tuples`, `Option` and `Either`
 - Support for generic types, e.g. `List[User]`, `Map[String, List[User]]`
 - Custom serializers support for user's specific types, etc. for Date serializations
 - Specific error messages, e.g. in case of json not matching given data type
 - Adding type information to JSON if there is no other type information available, e.g. to support collection of subtypes, e.g. `{"User":{"name":"John Doe"}}` instead of default `{"name":"John Doe"}`