## Development Mode

### Start Cider from Vim:

You'll need to install [vim-fireplace](https://github.com/tpope/vim-fireplace).

Then, `:Connect` to the REPL and run:

    :Piggieback (figwheel-sidecar.repl-api/repl-env)

### Start Cider from Emacs:

Put this in your Emacs config file:

```
(setq cider-cljs-lein-repl "(do (use 'figwheel-sidecar.repl-api) (start-figwheel!) (cljs-repl))")
```

Navigate to a clojurescript file and start a figwheel REPL with `cider-jack-in-clojurescript` or (`C-c M-J`)

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

```
lein less auto
```

### Run application:

```
lein clean
scripts/repl  # or `lein figwheel dev` if you don't want the REPL
```

Figwheel will automatically push cljs changes to the browser, and
the REPL will let you inspect and modify live state on the page.

Wait a bit, then browse to [http://localhost:8080](http://localhost:8080).

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

