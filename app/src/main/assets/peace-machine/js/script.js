$(function() {

    var $activeElem = null;
    var $rotatable = $('.rotatable');

    /** Touch / mouse position x */
    var targetX = 0;

    /** Touch / mouse position x */
    var targetY = 0;

    /** Translation of control before rotation dynamically applied */
    var controlTranslation = 'translate(-50%, 0%) ';

    /**
     * Interface object to pass control information to.
     * For example, provided by a mobile WebView
     */
    var interface = window["PeaceMachineInterface"];

    storage = window.localStorage;

    /**
     * When a rotatable element (knob) is touched, we set it as
     * the active element, all movement's on the ui will then be
     * forwarded to that element via the handleMove function until
     * the touch is lifted from the ui.
     */
    $rotatable.bind('mousedown touchstart', function(e) {
        e.stopPropagation();
        e.preventDefault();
        $activeElem = $(e.target);
    });

    /**
     * Once a control is active we want it to stay active even if
     * the mouse or finger leaves the element.
     */
    $rotatable.bind('mouseout touchend', function(e) {
        e.stopPropagation();
    });

    /**
     * Stop propogation of mouse out events on background as we
     * want a drag that started on one element to continue even
     * when the mouse passes out of the background to a different
     * element with the button still pressed.
     */
    $('#pm-screen-background').bind('mouseout', function(e) {
        e.stopPropagation();
    })

    /**
     * @param {event} e Check if event was touch not mouse
     */
    function isTouch(e) {
        return e.originalEvent && e.originalEvent.touches;
    }

    /**
     * Bind mouse/touch move events to the body. The position
     * is passed to the handleMove function wich will carry out
     * actions on any element that has been activated by a mouse
     * or touch down event.
     */
    $(document.body).bind('mousemove touchmove', function(e) {
        var touchInfo = e;
        if(isTouch(e)) {
            touchInfo = e.originalEvent.touches[0];
        }
        targetX = touchInfo.pageX;
        targetY = touchInfo.pageY;
        handleMove(targetX, targetY);
        e.stopPropagation();
    });

    /**
     * Converts canvas rotation to continuous cw rotation from 
     * 0 to 2pi.
     * 
     * @param {rot} rot canvas rotation (0 to pi ccw, 0 to -pi cw)
     */
    function canvasRotToCWRot(rot) {
        return physii.math.loneRanger(rot, -Math.PI, Math.PI, 0, Math.PI * 2);
    }

    /**
     * Converts canvas rotation to knob value between 0 and 1.
     * Val 0 is at 45deg cw from vertically down.
     * Val 1 is at 45deg ccw from vertically down.
     * @param {float} rot canvas rotation (0 to pi ccw, 0 to -pi cw)
     * @returns {float} Control value from 0 to 1
     */
    function rotToValue(rot) {
        var val = canvasRotToCWRot(rot);
        val = physii.math.loneRanger(val, Math.PI/4, 7*Math.PI/4, 0, 1);
        return val;
    }

    /**
     * Converts val from 0 to 1 to rotation in radians.
     * 
     * @param {float} val 
     */
    function valueToRot(val) {
        var cwRot = physii.math.loneRanger(val, 0, 1, Math.PI/4, 7 * Math.PI/4);
        var rot = physii.math.loneRanger(cwRot, 0, 2 * Math.PI, -Math.PI, Math.PI);
        console.log(JSON.stringify(['r', rot]));
        return rot;
    }

    /**
     * Handles mouse or touch movement.
     * 
     * @param {int} x Mouse/touch x pos
     * @param {int} y Mouse/touch y pos
     */
    function handleMove(x, y) {
        if($activeElem) {
            $elem = $activeElem;
            var w = $elem.width();
            var h = $elem.height();
            var relX = 0, relY = 0;
            var rect = $elem[0].getBoundingClientRect();
            var w = rect.width;
            var h = rect.height;

            var offset = $elem.offset(); 
            relX = x - (offset.left + w/2);
            relY = y - (offset.top + h/2);
            // Rotate 90 deg anticlockwise as atan2 uses x pointing right not up
            var relXRot = -relY;
            var relYRot = relX;

            var rot = Math.atan2(relYRot, relXRot);
            var cwRot = canvasRotToCWRot(rot);
            var val = rotToValue(rot);
            setValue($elem.attr("id"), val);
            
            if(rot > 3 * Math.PI / 4) {
                rot = 3 * Math.PI / 4;
            }
            else if(rot < -3 * Math.PI / 4) {
                rot = -3 * Math.PI / 4;
            }
           
            $elem.css('transform', controlTranslation + ' rotate(' + rot + 'rad)');

            console.log(JSON.stringify(['rel', relX, relY]));
        }
    }

    /**
     * Sets the controls to their default positions or values retrieved
     * from storage.
     */
    function initUI() {
        var ids = ["pm-control-uppers", "pm-control-downers"];

        for(var i = 0; i < ids.length; i++) {
            var id = ids[i];
            var $elem = $('#' + id);
            var val = getValue(id);
            var rot = valueToRot(val);
            $elem.css('transform', controlTranslation + ' rotate(' + rot + 'rad)');

            if(interface) {
                interface.handleFloat(id, val);
            }
        }
    }

    /**
     * Send a changed UI value to any connected interface. Save the value
     * for recovery across sessions.
     * 
     * @param {string} key 
     * @param {object} value 
     */
    function setValue(key, value) {
        storage.setItem(key, JSON.stringify(value));

        if(interface) {
            interface.handleFloat($elem.attr("id"), value);
        }
    }

    /**
     * Recover stored UI values from storage to initialize UI and any connected
     * interfaces.
     * 
     * @param {string} key 
     */
    function getValue(key) {
        var val = JSON.parse(storage.getItem(key));
        return val;
    }

    /**
     * Stop responding to move events once the touch or mouse is
     * lifted from the ui.
     */
    $(document.body).bind('mouseup mouseout', function(e) {
        console.log(JSON.stringify(['out']));
        $activeElem = null;
    });

    initUI();
});
