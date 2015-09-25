class Copy
  constructor: (data) ->
    @name = m.prop data.name
    @code = m.prop data.code
    @status = m.prop data.status

  @list: ->
    m.request(
      method: jsRoutes.controllers.Application.icjson().method
      url: jsRoutes.controllers.Application.icjson().url
      type: Copy
      initialValue: []
      background: false
    )

inventoryCheck =
  controller: ->
    @copies = Copy.list()
    @code = m.prop ""

    @copies.then(m.redraw)

    @check = () ->
      if !!@code()
        @copies().map (copy) =>
          if copy.code() is @code() then copy.status("available")
          copy
        @code("")

    return
  view: (ctrl) ->
    statusClass = (status) ->
      switch status
        when "loaned" then "info"
        when "available" then "success"
        else "danger"

    statusLabel = (status) ->
      if status is "loaned"
        m("span.label.label-#{statusClass(status)}", "Loaned")
      else if status is "available"
        m("span.label.label-#{statusClass(status)}", "Available")
      else
        m("span.label.label-#{statusClass(status)}", "Missing")

    summary = (copies) ->
      copies.reduce(
        (acc, copy) ->
          switch copy.status()
            when "loaned" then acc.loaned++
            when "available" then acc.available++
            else acc.missing++
          acc
        {available: 0, loaned: 0, missing: 0}
      )

    {available, loaned, missing} = summary(ctrl.copies())

    [
      m(".animated.fadeIn", [
        m("h1", "Inventory check")
        m("aside.well.well-sm.pull-right", [
          m("h4", "Summary")
          m("ul.list-unstyled", [
            m("li.text-success", [m("strong", available), " available"])
            m("li.text-info", [m("strong", loaned), " loaned"])
            m("li.text-danger", [m("strong", missing), " missing"])
          ])
        ])
        m("p", "
          Enter bar codes and the corresponding item will be marked green.
          Copies that are currently loaned will be marked blue and every other
          will be marked red. Any bar codes that the system does not recognize
          will show up in a list so they can be handled.
        ")
        m("p", [
          m("form", {onsubmit: (e) -> e.preventDefault(); ctrl.check()}, [
            m("div.input-group", [
              m("input[type=text].form-control", {onchange: m.withAttr("value", ctrl.code), value: ctrl.code()})
              m("span.input-group-btn", [
                m("button.btn.btn-default[type=submit]", "Check")
              ])
            ])
          ])
        ])
        m("table.table.table-striped", [
          m("thead", [
            m("tr", [
              m("th", "Barcode")
              m("th", "Product")
              m("th", "Status")
            ])
          ])
          m("tbody", [
            ctrl.copies().map (copy, index) ->
              m("tr.animated.fadeInUp", {class: statusClass(copy.status()), style: "animation-delay: #{index * 0.05}s"}, [
                m("td", copy.code())
                m("td", copy.name())
                m("td", statusLabel(copy.status()))
              ])
            m("tr", m("td[colspan=3]", "Loading...")) unless ctrl.copies().length
          ])
        ])
      ])
    ]

document.addEventListener 'DOMContentLoaded', -> m.mount(document.getElementById("inventoryCheck"), inventoryCheck)