layout = (component) ->
  {
    controller: () -> new layout.controller(component)
    view: layout.view
  }

layout.controller = (component) ->
  content: component.view.bind(this, new component.controller)

layout.view = (ctrl) ->
  m(".container", [
    m(".row", [
      m(".col-sm-9.animated.fadeIn", ctrl.content())
      m(".col-sm-3", [
        m("h4", "Products")
        m("ul", [
          m("li", m("a[href='/product/add']", {config: m.route}, "Add product"))
          m("li", m("a[href='/person']", {config: m.route}, "Person"))
          m("li", m("a[href='/inventoryCheck']", {config: m.route}, "Inventory check"))
        ])
      ])
    ])
  ])

exports = this

document.addEventListener "DOMContentLoaded", () ->
  m.route(document.body, "/inventoryCheck", {
    "/person": layout(exports.person.search)
    "/person/:id": layout(exports.person.view)
    "/inventoryCheck": layout(exports.inventoryCheck)
    "/product/add": layout(exports.product.add)
  })