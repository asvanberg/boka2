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
    @person = exports.Person.get(m.route.param("id"))
    return
  view: (ctrl) -> [
    m "h1", "Person information"
    if not ctrl.person()
      m "p", "No person found"
    else
      m.component exports.components.details.person, ctrl.person
  ]

exports.person =
  search: search
  view: personComponent
