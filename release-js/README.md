# DataScript-Async

Forked the query language from [Datascript](https://github.com/tonsky/datascript), and changed to only utilize external indexes in an asynchronous manner.

# To-Do

- More documentation.
- More examples.
- More tests.

# Usage

For instance, say you have a PouchDB database with two views, one called `eav/eav` and one called `ave/ave`. These views would have array keys, such as [entity,attribute,value] and [attribute,value,entity], respectively, and both (for ease in our example) would have values of [entity,attribute,value]. (Another way of thinking of these triples is [id,label,value].)

Then, you could query the database as follows:

```javascript
var datascript = require( "dataScript-async" );
var PouchDB = require( "pouchdb" );
var db = new PouchDB( "datascript" );

var searchPouchIndex = function (db, index) {
  return function (startkey, endkey, index_callback) {
    var view = index + "/" + index;
    if (index === "eav") {
      startkey = [startkey.e, startkey.a, startkey.v];
      endkey = [endkey.e, endkey.a, endkey.v];
    } else if (index === "ave") {
      startkey = [startkey.a, startkey.v, startkey.e];
      endkey = [endkey.a, endkey.v, endkey.e];
    }
    endkey = endkey.map(function (el) {
      if (el === null) {
        return {};
      }
      return el;
    });
    return db.query(view, {
      startkey: startkey,
      endkey: endkey
    }).then(function (data) {
      var result = data.rows.map(function (el) {
        return el.value;
      });
      index_callback(result);
    }).catch(function (error) {
      console.error("An error occured.", error);
    });
  };
};

db = datascript.set_indexes(searchPouchIndex(self, "eav"), searchPouchIndex(self, "ave"));

datascript.q(callback, '[:find ?id :in :where [?id "last_name" "benson"]]', function( data ) {
  console.log( "Query results: ", data );
}, db );

```

In fact, as long as you can provide functions to search the "eav" and "ave" indexes on any dataset, returning "eav" triples, you can use Dataquery to query those indexes. This should include in-memory indexes (though I'd suggest using [Datascript](https://github.com/tonsky/datascript) as it has been optimized for that use-case), IndexedDB, or pretty much any persistent store that would allow you to define indexes as necessary.

# Installation

```
npm install --save datascript-async
```

# Status

Quite alpha. Contributions/suggestions/constructive critique very welcome!
