(ns ^{:author "Daniel Leong"
      :doc "Provider protocol"}
  sheater.provider.proto)

(defprotocol IProvider
  (create-sheet
    [this info on-complete]
    "Create a new sheet with the given info asynchronously,
    calling on-complete when done. on-complete should be
    provided with an Error instance or a sheet map. Info
    is a map that should contain:
    - :name  The name of the sheet")
  (delete-sheet
    [this info]
    "Delete a sheet")
  (refresh-sheet
    [this info on-complete]
    "Fetch the contents of a sheet if we don't already have it.
    on-complete should be called with the contents as edn")
  (save-sheet
    [this info on-complete]
    "Save changes to a sheet. on-complete is called with an Error
     on failure, or a falsey value on success"))
