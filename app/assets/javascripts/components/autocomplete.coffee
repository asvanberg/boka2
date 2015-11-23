exports = this

debounce = (func, threshold, execAsap) ->
  timeout = null
  (args...) ->
    obj = this
    delayed = ->
      func.apply(obj, args) unless execAsap
      timeout = null
    if timeout
      clearTimeout(timeout)
    else if (execAsap)
      func.apply(obj, args)
    timeout = setTimeout delayed, threshold or 100

person =
  controller: ->
    list = m.prop([])
    lastSearch = m.prop ""
    {
      selectedIndex: m.prop 0
      list: list
      search: debounce(
        (term) ->
          exports.Person.search(term).then(list) unless term == lastSearch()
          lastSearch term
        300
      )
    }

  view: (ctrl, args) ->
    m "div", [
      m "input[placeholder=Search...].form-control",
        config: (el, init) -> if not init then el.focus()
        onkeyup: m.withAttr("value", ctrl.search)
        onkeydown: (e) ->
          idx = ctrl.selectedIndex()
          switch e.keyCode
            when 38 then ctrl.selectedIndex(Math.max(idx - 1, 0))
            when 40 then ctrl.selectedIndex(Math.min(idx + 1, 9, ctrl.list().length - 1))
            when 13
              person = ctrl.list()[idx]
              args.onselect person if person
          e.preventDefault() if e.keyCode is 38 or e.keyCode is 40
      m ".list-group", [
        ctrl.list().slice(0, 10).map (person, index) ->
          m "a.list-group-item",
            onclick: args.onselect.bind this, person
            key: person.id()
            class: if index == ctrl.selectedIndex() then "active" else ""
            [
              m.component exports.components.image.person,
                class: "pull-right img-rounded"
                person: person
                style: "max-height: 44px"
                onerror: (e) -> e.target.style.display = "none"
              m "h4.list-group-item-heading", "#{person.firstName()} #{person.lastName()}"
              m "small.list-group-item-text", "<#{person.email()}>" if person.email()
              m ".clearfix"
            ]
      ]
    ]

exports.components ||= {}
exports.components.autocomplete ||= {}
exports.components.autocomplete.person = person