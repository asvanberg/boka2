class Person
  constructor: (data) ->
    @id = m.prop data.id
    @firstName = m.prop data.firstName
    @lastName = m.prop data.lastName
    @email = m.prop data.email

  @get: (id) ->
    m.request(
      method: jsRoutes.controllers.Application.specificPerson(id).method
      url: jsRoutes.controllers.Application.specificPerson(id).url
      type: Person
      extract: (xhr) ->
        if xhr.status == 200 then xhr.responseText
        else xhr.status
    )

  @search: (term) ->
    m.request(
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

search =
  controller: ->
    list = m.prop([])
    {
      list: list
      search: debounce(
        (term) -> Person.search(term).then(list)
        300
      )
    }

  view: (ctrl) ->
    [
      m("h1", "Person information")
      m("input.form-control", {onkeyup: m.withAttr("value", ctrl.search)})
      m ".list-group", [
        ctrl.list().slice(0, 10).map (person) ->
          m "a.list-group-item", {href: "/person/#{person.id()}", config: m.route}, [
            m "img.pull-right", {src: "/admin/person.photo?id=#{person.id()}", style: "max-height: 40px"}
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
        m("img.pull-right.img-rounded", {src: "/admin/person.photo?id=#{ctrl.person().id()}"})
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