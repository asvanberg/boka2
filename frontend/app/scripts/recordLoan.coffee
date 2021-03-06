exports = this

Copy = exports.Copy
Product = exports.Product

class CopyDetails
  constructor: (data) ->
    @copy = m.prop new Copy data.copy
    @product = m.prop new Product data.product
    @status = m.prop data.status

  @get: (barcode) ->
    m.secureRequest
      method: "GET"
      url: "/api/copy/details/#{barcode}"
      type: CopyDetails
      background: true

(exports.loan ||= {}).recordLoan =
  controller: ->
    @person = m.prop null
    @pending = m.prop []
    @copies = m.prop []
    @barcode = m.prop ""
    @message = m.prop ""

    @addItem = =>
      @pending().push(@barcode())
      if not @copies().some((c) => c.copy().barcode() is @barcode())
        CopyDetails.get(@barcode())
          .then(
            (c) =>
              @copies().push(c)
              @pending().splice(@pending().indexOf(c.copy().barcode()), 1)
            ((barcode) ->
              @message "No item with barcode '#{barcode}'"
              @pending().splice(@pending().indexOf(barcode), 1)
            ).bind this, @barcode()
          )
          .then m.redraw
      @barcode ""

    @recordLoans = =>
      record = (c) =>
        c.copy().borrow(@person())
          .then(
            -> c.status "ok"
            -> c.status "error"
          )
      record c for c in @copies()

    return
  view: (ctrl) -> [
    m "h1", "Record new loan"
    m "ul", [
      m "li", [
        m "h2", "Select borrower"
        if not ctrl.person()
          m.component exports.components.autocomplete.person,
            onselect: ctrl.person
        else [
          m.component exports.components.details.person, ctrl.person
          m "button.btn.btn-link",
            onclick: -> ctrl.person null
            "Select another borrower"
          m ".clearfix"
        ]
      ]
      m "li", [
        m "h2", "Scan items"
        if ctrl.message()
          m ".alert.alert-danger", [
            m "button.close",
              onclick: ctrl.message.bind this, ""
              "×"
            ctrl.message()
          ]
        m ".form-group", [
          m "label.sr-only[for=barcode]", "Enter barcode"
          m ".input-group", [
            m "input.form-control[placeholder=Scan items...][id=barcode]",
              onchange: m.withAttr "value", ctrl.barcode
              value: ctrl.barcode()
            m "span.input-group-btn",
              m "button.btn.btn-default",
                onclick: ctrl.addItem
                "Add"
          ]
        ]
        if ctrl.copies().length or ctrl.pending().length
          m "table.table.table-striped.table-hover", [
            m "thead", [
              m "tr", [
                m "th", "Barcode"
                m "th", "Name"
                m "th", m.trust "&nbsp;"
              ]
            ]
            m "tbody", [
              ctrl.copies().map (details) ->
                m "tr",
                  key: details.copy().barcode()
                  class: switch details.status()
                    when "borrowed" then "warning"
                    when "ok" then "success"
                    when "error" then "danger"
                    else ""
                  [
                    m "td", details.copy().barcode()
                    m "td", details.product().name()
                    m "td.text-right",
                      switch details.status()
                        when "borrowed"
                          m "a.right.animate[data-tooltip=This item is already out on loan]", m "span.glyphicon.glyphicon-warning-sign.text-warning"
                        when "ok"
                          m "span.glyphicon.glyphicon-ok.text-success"
                        when "error"
                          m "a.right.animate[data-tooltip=This item is already out on loan]", m "span.glyphicon.glyphicon-remove.text-danger"
                        else m.trust "&nbsp;"
                  ]
              ctrl.pending().map (barcode) ->
                m "tr",
                  key: barcode
                  [
                    m "td", barcode
                    m "td[colspan=2]", m "span.loading", m "span", "●"
                  ]
            ]
          ]
      ]
    ]
    m "button.btn.btn-primary",
      onclick: ctrl.recordLoans
      disabled: not ctrl.copies().length or not ctrl.person()
      "Record"
  ]

(exports.loan ||= {}).returnLoan =
  controller: ->
    @person = m.prop null
    @barcode = m.prop ""
    @pending = m.prop []
    @copies = m.prop []
    @message = m.prop ""

    @addBarcode = =>
      @pending().push(@barcode())
      if not @copies().some((c) => c.copy().barcode() == @barcode())
        CopyDetails.get(@barcode())
          .then(
            (c) =>
              @copies().push(c)
              @pending().splice(@pending().indexOf(c.copy().barcode()), 1)
            ((barcode) ->
              @message "No item with barcode '#{barcode}'"
              @pending().splice(@pending().indexOf(barcode), 1)
            ).bind this, @barcode()
          )
          .then m.redraw
      @barcode ""

    @returnItems = =>
      for c in @copies()
        do (c) ->
          c.copy().return_(Date.now())
            .then(
              -> c.status "ok"
              -> c.status "error"
            )

    return
  view: (ctrl) -> [
    m "h1", "Return items"
    m "ul", [
      m "li", [
        m "h2", "Scan items"
        if ctrl.message()
          m ".alert.alert-danger", [
            m "button.close",
              onclick: ctrl.message.bind this, ""
              "×"
            ctrl.message()
          ]
        m ".form-group", [
          m "label.sr-only[for=barcode]", "Enter barcode"
          m ".input-group", [
            m "input.form-control[placeholder=Scan items...][id=barcode]",
              onchange: m.withAttr "value", ctrl.barcode
              value: ctrl.barcode()
            m "span.input-group-btn",
              m "button.btn.btn-default",
                onclick: ctrl.addBarcode
                "Add"
          ]
        ]
        if ctrl.copies().length or ctrl.pending().length
          m "table.table.table-striped.table-hover", [
            m "thead", [
              m "tr", [
                m "th", "Barcode"
                m "th", "Name"
                m "th", m.trust "&nbsp;"
              ]
            ]
            m "tbody", [
              ctrl.copies().map (details) ->
                m "tr",
                  key: details.copy().barcode()
                  class: switch details.status()
                    when "available" then "warning"
                    when "ok" then "success"
                    when "error" then "danger"
                    else ""
                  [
                    m "td", details.copy().barcode()
                    m "td", details.product().name()
                    m "td.text-right",
                      switch details.status()
                        when "available"
                          m "a.right.animate[data-tooltip=This item is not out on loan]", m "span.glyphicon.glyphicon-warning-sign.text-warning"
                        when "ok"
                          m "span.glyphicon.glyphicon-ok.text-success"
                        when "error"
                          m "a.right.animate[data-tooltip=This item is not out on loan]", m "span.glyphicon.glyphicon-remove.text-danger"
                        else m.trust "&nbsp;"
                  ]
              ctrl.pending().map (barcode) ->
                m "tr",
                  key: barcode
                  [
                    m "td", barcode
                    m "td[colspan=2]", m "span.loading", m "span", "●"
                  ]
            ]
          ]
      ]
    ]
    m "button.btn.btn-primary",
      onclick: ctrl.returnItems
      disabled: not ctrl.copies().length
      "Return"
  ]
