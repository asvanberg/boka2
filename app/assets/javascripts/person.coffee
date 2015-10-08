class Person
  constructor: (data) ->
    @id = m.prop data.id
    @firstName = m.prop data.firstName
    @lastName = m.prop data.lastName
    @email = m.prop data.email

  @get: (id) ->
    m.secureRequest(
      method: jsRoutes.controllers.Application.specificPerson(id).method
      url: jsRoutes.controllers.Application.specificPerson(id).url
      type: Person
      extract: (xhr) ->
        if xhr.status is 200 then xhr.responseText
        else xhr.status
    )

  @search: (term) ->
    m.secureRequest(
      method: jsRoutes.controllers.Application.search().method
      url: jsRoutes.controllers.Application.search(term).url
      type: Person
    )

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

image =
  controller: (args) ->
    @imgData = m.secureRequest(
      method: "GET"
      url: "/admin/person.photo?id=#{args.person.id()}"
      deserialize: (a) -> a
      background: true
    )
    @imgData.then(m.redraw)
    return
  view: (ctrl, args) ->
    args ||= {}
    args.src = "data:image/jpeg;base64,#{ctrl.imgData()}" if ctrl.imgData()
    m "img", args

search =
  controller: ->
    list = m.prop([])
    lastSearch = m.prop ""
    {
      selectedIndex: m.prop 0
      list: list
      search: debounce(
        (term) ->
          Person.search(term).then(list) unless term == lastSearch()
          lastSearch term
        300
      )
    }

  view: (ctrl) ->
    [
      m("h1", "Person information")
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
              m.route "/person/#{person.id()}" if person
          e.preventDefault() if e.keyCode is 38 or e.keyCode is 40
      m ".list-group", [
        ctrl.list().slice(0, 10).map (person, index) ->
          m "a.list-group-item",
            href: "/person/#{person.id()}"
            config: m.route
            key: person.id()
            class: if index == ctrl.selectedIndex() then "active" else ""
            [
              m.component image,
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

personComponent =
  controller: ->
    person = Person.get(m.route.param("id"))

    {
      person: person
    }
  view: (ctrl) ->
    m("div", [
      m("h1", "Person information")
      m("p", "No person found") unless ctrl.person()
      if ctrl.person() then m(".animated.fadeIn", [
        m.component image,
          class: "pull-right img-rounded"
          person: ctrl.person()
        m("dl", [
          m("dt", "First name")
          m("dd", ctrl.person().firstName())
          m("dt", "Last name")
          m("dd", ctrl.person().lastName())
          m("dt", "Email")
          m("dd", ctrl.person().email())
        ])
      ])
    ])

exports = this
exports.person =
  search: search
  view: personComponent