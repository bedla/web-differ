let deleteWebPage = function (webPageId, webPageName, successAction) {
    let name = $("<i>").text(webPageName).html();
    bootbox.confirm({
        title: 'Delete WebPage?',
        message: `Do you really want to delete WebPage '${name}'!`,
        buttons: {
            cancel: {
                label: 'No',
                className: 'btn-light'
            },
            confirm: {
                label: 'Delete',
                className: 'btn-danger'
            }
        },
        callback: function (result) {
            if (result) {
                $.ajax({
                    type: "DELETE",
                    url: `/api/user/me/web-page/${webPageId}`,
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    success: function (data, textStatus, jqXHR) {
                        successAction(data, textStatus, jqXHR);
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        onError(jqXHR, textStatus, errorThrown, jQuery.noop, {
                            404: function () {
                                bootbox.alert('WebPage not found.');
                            },
                            '*': function (defaultErrorMessage) {
                                bootbox.alert(defaultErrorMessage);
                            }
                        });
                    }
                })
            }
        }
    });
};

let executeWebPage = function (webPageId, webPageName, successAction) {
    let name = $("<i>").text(webPageName).html();
    bootbox.confirm({
        title: 'Execute WebPage monitor?',
        message: `Do you really want to execute change detection on WebPage '${name}'!`,
        buttons: {
            cancel: {
                label: 'No',
                className: 'btn-light'
            },
            confirm: {
                label: 'Execute',
                className: 'btn-primary'
            }
        },
        callback: function (result) {
            if (result) {
                $.ajax({
                    type: "POST",
                    url: `/api/user/me/web-page/${webPageId}/execute`,
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    success: function (data, textStatus, jqXHR) {
                        successAction(data, textStatus, jqXHR);
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        onError(jqXHR, textStatus, errorThrown, jQuery.noop, {
                            404: function () {
                                bootbox.alert('WebPage not found.');
                            },
                            '*': function (defaultErrorMessage) {
                                bootbox.alert(defaultErrorMessage);
                            }
                        });
                    }
                })

            }
        }
    });
};

let onError = function (jqXHR, textStatus, errorThrown, actionBefore, actions) {
    actionBefore(jqXHR, textStatus, errorThrown);
    console.log(jqXHR.status);
    console.log(jqXHR.responseText);
    console.log(textStatus);
    console.log(errorThrown);
    let defaultErrorMessage = "Don't panic, there is error in log.";
    let perStatusAction = actions[jqXHR.status];
    if (perStatusAction === undefined) {
        let genericAction = actions['*'];
        if (genericAction === undefined) {
            alert(defaultErrorMessage);
        } else {
            genericAction(defaultErrorMessage, jqXHR, textStatus, errorThrown)
        }
    } else {
        perStatusAction(defaultErrorMessage, jqXHR, textStatus, errorThrown)
    }
};

let tableRow = function (tBody, text, colSpan) {
    tBody.find("tr").remove();
    tBody
        .append($('<tr>')
            .append($('<td>')
                .attr('colspan', colSpan)
                .text(text)
            )
        );
};

function coalesce() {
    let len = arguments.length;
    for (let i = 0; i < len; i++) {
        if (arguments[i] !== null && arguments[i] !== undefined) {
            return arguments[i];
        }
    }
    return null;
}

let formatZonedDateTime = function (value) {
    let result = moment(value, 'YYYY-MM-DDTHH:mm:ss:SSZ');
    return result.isValid() ? result.format("YYYY-MM-DD HH:mm:ss") : '';
};
