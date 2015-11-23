exports = this

search =
  controller: ->
  view: ->
    [
      m("h1", "Person information")
      m.component exports.components.autocomplete.person,
        onselect: (person) -> m.route "/person/#{person.id()}" if person
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
        m.component exports.components.image.person,
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

exports.person =
  search: search
  view: personComponent