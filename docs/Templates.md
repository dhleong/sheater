# Sheater Templates

This document assumes you have a working knowledge of [hiccup][1] and
[EDN][2]. Knowledge of [clojure][3]/[clojurescript][4] is recommended
for maximum awesome, but you can make basic templates without it.

## Basic layout

A template looks like this:

```clojure
{:static {}
 :pages [{:name "Main"
          :type :notes}]}
```

`:static` is a key-value map which can contain arbitrary data used
elsewhere (More on that later). `:pages` is a vector of Page structures,
which are just maps, as you can see above.

## Pages

A page is a map with a `:name` and a `:type`. If `:type` is omitted,
it is assumed to be a custom page, and must provide a `:spec`. See
[Custom Pages](#custom-pages) below.

### Page Types

Currently, Sheater only provides one built-in page type.

#### `:notes` Page

The `:notes` page type provides a searchable note-taking interface
that supports #tags for quick filtering. It is not customizable,
but it is pretty powerful, and can be automatically upgraded with
fixes and enhancements by the server without any effort on your part.
Notes are stored in a special `:sheater/notes` key in the user data.
Multiple `:notes` pages are supported, if you're into that.

## Custom Pages

Custom pages are Sheater's bread and butter. The page map for a custom
page omits the `:type` key and instead has a `:spec` key, which is a
[hiccup][1] vector... plus some extra magic.

### Auto-Input

A key component to Sheater custom pages is the notion of the "auto-input."
Any widget (see below) that supports an ID will automatically save
its value to the user data map with its ID as the key. For example:

```clojure
[:input#name]
```

This is the most basic widget in sheater, the standard text `:input`,
with the ID "name." When the user inputs their character's name into
this field, let's say "Malcolm Reynolds" for example, the user data
map will be updated to look like:

```clojure
{:name "Malcolm Reynolds"}
```

When the sheet is re-loaded later, the user data will be automatically
filled into the matching auto-input fields.

In addition to the convenience syntax above, the ID for an auto-input
widget can be supplied manually in the attributes map, just like with hiccup. The above example, then, can be re-written as:

```clojure
[:input {:id :name}]
```

See [Dynamic Auto-Inputs](#dynamic-auto-inputs) below for an example
of when this might be useful.

### Auto-Value

The other key component to Sheater custom pages are auto-values.
Auto-values are inline bits of Clojure(script) code that provide
state and values for your templates. For example, say you want to
calculate someone's "Defence" stat for them. If that's simply
10 + dexterity, you might do something like this:

```clojure
[:div "Defence: " (+ 10 :#dex)]
```

Note the [Clojure][3] form in there, and the weird keyword. That
is a reference to an auto-input field that may look like
`[:input#dex.number]` somewhere else on the page (or another page).

You can similarly reference static data by prefixing the key with
a `$`, for examples `:$classes`.

Both auto-value key types can be combined with Clojure to provide
some powerful functions, like this, from the FantasyAGE template:

```clojure
(- (+ (get-in :$races [:#race :speed]) :#dex) :#armor-penalty)
```

Here, `$races` is a static map which, in context, looks like:

```clojure
{:static {:races {"Elf" {:speed 12}}}}
```

Since `:#race` will expand to the user's auto-input Race name,
the above snippit will reach into the `"Elf"` key of the `:races`
static data, add it with the `:#dex` auto-input, and subtract the
`:#armor-penalty` auto-input.

#### Dynamic Auto-Inputs

By combining the above inputs, we can dynamically generate auto-inputs.
Say you have a list of abilities in your static data. Rather than
creating a table with a row for each one by hand, why not generate it?
Here's an except from the FantasyAGE template:

```clojure
     [:table
      [:tbody
       [:tr
        [:th
         "P?"]
        [:th
         "Ability"]
        [:th
         "Focuses"]
        [:th
         "Rating"]]
       (for [a :$abilities]
         [:tr
          [:td
           [:checkbox
            {:value
             (contains?
               (get-in
                 :$classes
                 [:#class
                  :primaries])
               (:id a))}]]
          [:td
           (:label a)]
          [:td
           [:selectable-set
            {:id (keyword (str (name (:id a)) "-focuses"))
             :items (get :$focuses (:id a))}]]
          [:td
           [:input.number {:id (:id a)}]]])]]
```

The `:abilities` static data is a vector that looks like:

```clojure
 {:abilities
  [{:id :acc, :label "Accuracy"}
   {:id :int, :label "Intelligence"}]}
```

Note the `for`-loop over the entries in the static `:abilities`
vector, the auto-value on the `:checkbox`, and accessing the
`:label` key on the ability map. Note also how we manually
specifiy the widget ID on the `:input.number` at the end, instead
of the normal auto-input syntax. This is used on the `:selectable-set`
of ability focuses just above, as well, where we dynamically create
the auto-input ID based on the ability's `:id`.

### Widgets

Sheater templates expand the normal [hiccup][1] syntax to include
custom elements, called "widgets," in addition to the regular HTML
ones like `:div`.

Many widgets require an ID and are only auto-input, but some are marked
as (optional ID), and can function as auto-value. The auto-value should
be provided in the `:value` key of the attributes map, unless otherwise
specified.

Here's a quick list:

- `:checkbox` A checkbox. (optional ID)
- `:cols` Convenience to place content side-by-side in a [responsive][5]
    way. On smaller devices, the columns will collapse to become
    top-to-bottom rows. (layout only)
- `:consumables` A convenience for a `:dynamic-table` (requires ID)
- `:currency` A small widget that lets you keep track of how much
    currency you're carrying, and which will eventually support
    conversions between units. (requires ID)
- `:input` The basic text input widget. Can be restricted to numbers
    using the `.number` or `.big-number` classes. (requires ID)
- `:input-calc` A fancier `input.number` (or `.big-number`, if you
    use that class) that pops up a prompt to add or subtract an amount,
    so you don't have to. (requires ID)
- `:inventory` A convenience for a `:dynamic-table` that represents a
    container of items, which will eventually support drag-and-drop
    between other `:inventory` widgets. (requires ID)
- `:picker` A drop-down that lets the user select between items in
    the `:items` key of the attribute map. (requires ID)
- `:table` Wrapper around the standard HTML table that provides some
    automatic styling when it has a single row (or a single data row
    and a single header row). (layout only)
- `:dynamic-table` A beast of a widget that expects a `:cols` vector
    with colum labels for the rows, which can be manually entered by
    the user in a prompt. You may provide an `:items` auto-value to
    give the user options to choose from as well. Furthermore, you
    may provide a `:value` auto-value of rows that are automatically
    added (and un-deletable). See the "spells" dynamic table in the
    FantasyAGE template for an example. (requires ID)
- `:partial-number` A convenient wrapper of an `input.number` and
    an `input-calc` that can be used for anything with a "current"
    and a "max" value, such as HP. The "max" value is stored in
    a key that is the ID provided to this element with "-max"
    appended (requires ID).
- `:selectable-set` A horizontally-flowing set of items that the
    user picks from a list specified by `:items`. (requires ID)
- `:selectable-list` A vertically-flowing set of items that the
    user picks from a list specified by `:items`. (requires ID)


[1]: https://github.com/weavejester/hiccup
[2]: https://github.com/edn-format/edn
[3]: https://clojure.org/
[4]: https://clojurescript.org/
[5]: https://www.w3schools.com/html/html_responsive.asp
