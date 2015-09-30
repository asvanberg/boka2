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
        (term) ->
          selected = [].slice.call(document.getElementById("people").options).find (option) ->
             option.value is term
          if selected
            m.route("/person/#{selected.getAttribute("pid")}")
          else
            Person.search(term).then(list)
        300
      )
    }

  view: (ctrl) ->
    [
      m("h1", "Person information")
      m("input.form-control[list=people]", {onkeyup: m.withAttr("value", ctrl.search)})
      m("datalist#people", [
        ctrl.list().slice(0, 10).map (person) ->
          m(
            "option[value=#{person.firstName()} #{person.lastName()} <#{person.email()}>]"
            {pid: person.id(), key: person.id()}
          )
      ])
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