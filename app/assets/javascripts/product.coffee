class Product
  constructor: (data) ->
    @id = m.prop data?.id
    @name = m.prop data?.name
    @description = m.prop data?.description

productModule =
  add:
    controller: () ->
      @error = m.prop {}
      @product = m.prop new Product
      @add = (product) =>
        m.request
          method: jsRoutes.controllers.Application.doAddProduct().method
          url: jsRoutes.controllers.Application.doAddProduct().url
          data: product
          type: Product
        .then @product, @error
      return @
    view: (ctrl) ->
      [
        m "h1", "Add product"
        m.component productModule.form, {error: ctrl.error, onsave: ctrl.add}
      ]
  list:
    controller: () ->
    view: (ctrl) ->

  form:
    controller: (args) ->
      @product = args.product || m.prop new Product
      @error = args.error || m.prop {}
      return @
    view: (ctrl, args) ->
      product = ctrl.product()
      m "form", {onsubmit: (e) -> e.preventDefault(); args.onsave(product)}, [
        if ctrl.error().fields then m ".alert.alert-danger", [
          m "ul", [m "li", message for _, message of ctrl.error().fields]
        ]
        m ".form-group", {class: if ctrl.error().fields?.name then "has-error" else ""}, [
          m "label[for=name].control-label", "Name"
          m "input#name[type=text][].form-control", {oninput: m.withAttr("value", product.name)}
        ]
        m ".form-group", {class: if ctrl.error().fields?.description then "has-error" else ""}, [
          m "label[for=description].control-label", "Description"
          m "textarea#description.form-control", {oninput: m.withAttr("value", product.description)}
        ]
        m "button[type=submit].btn.btn-primary", "Save"
      ]

exports = this
exports.product =
  add: productModule.add
