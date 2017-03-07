$(document).ready(function(){ ajaxindicatorinit('loading data.. please wait..'); });

function GetListItems(opts){
    var defaultOpt = {
        operation : "GetListItems"
    }
    opts = $.extend(defaultOpt,opts);
    $().SPServices(opts);
}

function GetListItemsJson(opts,cb){
    var data = $().SPServices.SPGetListItemsJson(opts);
    $.when(data).done(cb);
}
function findUsername(){ //TODO check if it populates variable in MissionReport.js
    username = $().SPServices.SPGetCurrentUser({
        fieldName: "Name",
        debug: false
    });
    getUsersOperator(function(o){
        operator = o;
    });
}
function saveListItem(list,obj,cb,cbArgs){
    if(obj.ID == null || obj.ID == '')
        insertListItem(list, obj, cb,cbArgs);
    else
        updateListItem(list, obj, obj.ID, cb,cbArgs);
}
function getKeyValuePairs(obj){
    var keys = Object.keys(obj);
    var values = [];
    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        value = noNull(obj[key]);
        if (value instanceof Date)
            value = formatDate(value);
        else if(typeof value.lookupId != 'undefined'){
            value = value.lookupId + ';#'+value.lookupValue;
        }
        else if(value instanceof Array && value.length > 0 && typeof value[0].lookupId != 'undefined'){
            var v = '';
            value.forEach(function(t){
                v = v+';#'+t.lookupId + ';#'+t.lookupValue;
            });
            value = v.substring(2);
        }
        if(key != 'ID')
            values.push([ key, value ]);
    }
    return values;
}

function insertListItem(list, obj, cb,cbArgs) {
    var values = getKeyValuePairs(obj);
    loading();
    $()
        .SPServices(
        {
            operation : "UpdateListItems",
            async : false,//attempt to fix insert issue
            batchCmd : 'New',
            listName : list,
            valuepairs : values,
            completefunc : function(xData, status) {
                loadingDone();
                var result = $(xData.responseXML).SPFilterNode("Result")[0]
                var resultCode = $(result).find('ErrorCode');
                if (resultCode.text() == '0x00000000') {
                    if(cb) {
                        // get ID
                        //createdItemResponse = $(xData.responseXML).find("z\\:row")[0];// $(xData.responseXML).find("*").filter("z:row")
                        createdItemResponse = $(xData.responseXML).SPFilterNode("Result").children()[2];
                        cb(xData, status, cbArgs)
                    }
                } else {
                    var errorText = $(result).find('ErrorText').text()
                    confirmModal('<strong>ERROR UPDATING '
                        + list.toUpperCase()
                        + '</strong>'
                        + +'<br/>Please submit feedback with what you were changing, along with the details<br/>'
                        + resultCode.text() + ' - ' + errorText)
                }
            }
        });
}

function updateListItem(list, obj, id, callback,cbArgs,async) {
    async = typeof async !== 'undefined' ?  async : true;
    var values = getKeyValuePairs(obj);

    loading();
    $()
        .SPServices(
        {
            operation : "UpdateListItems",
            async : async,
            batchCmd : 'Update',
            listName : list,
            ID : id,
            valuepairs : values,
            completefunc : function(xData, status) {
                loadingDone();
                var result = $(xData.responseXML).SPFilterNode("Result")[0];
                var resultCode = $(result).find('ErrorCode');
                if (resultCode.text() == '0x00000000') {
                    if(callback)
                        callback(xData, status,cbArgs)
                } else {
                    var errorText = $(result).find('ErrorText').text()
                    confirmModal('<strong>ERROR UPDATING '
                        + list.toUpperCase()
                        + '</strong>'
                        + +'<br/>Please submit feedback with what you were changing, along with the details<br/>'
                        + resultCode.text() + ' - ' + errorText)
                }

            }
        });
}

function deleteListItem(listName, spid, cb) {
    loading();
    $()
        .SPServices(
        {
            operation : "UpdateListItems",
            listName : listName,
            batchCmd : "Delete",
            ID : spid,
            completefunc : function(xData, status) {
                loadingDone();
                var result = $(xData.responseXML).SPFilterNode("Result")[0]
                var resultCode = $(result).find('ErrorCode');
                if (resultCode.text() == '0x00000000') {
                    cb(xData, status)
                } else {
                    var errorText = $(result).find('ErrorText').text()
                    confirmModal('<strong>ERROR UPDATING '
                        + listName.toUpperCase()
                        + '</strong>'
                        + +'<br/>Please submit feedback with what you were changing, along with the details<br/>'
                        + resultCode.text() + ' - ' + errorText)
                }
            }
        });
}

function confirmDelete(objName, cb, $btn) {
    confirmModal("Are you sure you want to delete this " + objName + '?', cb, $btn)
}

function confirmModal(msg, cb, cbArg) {
    $("#dialog-confirm").html(msg);

    // Define the Dialog and its properties.
    $("#dialog-confirm").dialog({
        resizable : false,
        modal : true,
        title : "Confirm",
        height : 250,
        width : 400,
        buttons : {
            "Yes" : function() {
                $(this).dialog('close');
                if (cb)
                    cb(cbArg);
            },
            "No" : function() {
                $(this).dialog('close');
            }
        }
    });
}


function notifyErrorModal(msg, cb, cbArg) {
    $("#dialog-error").html(msg);

    // Define the Dialog and its properties.
    $("#dialog-error").dialog({
        resizable : false,
        modal : true,
        // title : "Confirm",
        height : 250,
        width : 400,
        buttons : {
            "Close" : function() {
                $(this).dialog('close');
                if (cb)
                    cb(cbArg);
            }
        }
    });
}
function handleFileChange(listName,itemId,files){
    var filereader = {},
        file = {},
        i=0;

    //loop over each file selected
    for(i = 0; i < files.length; i++) {
        file = files[i];

        filereader = new FileReader();
        filereader.filename = file.name;

        filereader.onload = function() {
            var data = this.result,
                n=data.indexOf(";base64,") + 8;

            //removing the first part of the dataurl give us the base64 bytes we need to feed to sharepoint
            data= data.substring(n);

            $().SPServices({
                operation: "AddAttachment",
                listName: listName,
                asynch: false,
                listItemID:itemId,
                fileName: this.filename,
                attachment: data,
                completefunc: function (xData, Status) {
                    console.log('attachment upload complete',xData,status);
                }
            });
        };

        filereader.onabort = function() {
            alert("The upload was aborted.");
        };

        filereader.onerror = function() {
            alert("An error occured while reading the file.");
        };

        //fire the onload function giving it the dataurl
        filereader.readAsDataURL(file);
    }
};

function clearFields(listName,$scope){
    $scope = typeof $scope == 'undefined' ?$(document):$scope;
    var inputList = $scope.find('[list="'+listName+'"]');
    inputList.each(function() {
        var input = $(this);
        var tag = input.prop("tagName");
        if(tag == 'INPUT' || tag == 'TEXTAREA') {
            input.val('');
            if(input.attr('type') == 'checkbox')
                input.prop('checked',false);
        }
        else if(tag == 'SELECT') {
            input.val('');
        }
        else if(tag == 'SPAN' || tag == 'DIV'){
            input.text('');
        }
    });
    if($().selectpicker)
        $('select[list="'+listName+'"].selectpicker').selectpicker('refresh');
}

function scrapeListItemObject(list,$scope){
    $scope = typeof $scope !== 'undefined' ?  $scope : $(document);
    var obj = {};
    $scope.find('[list="'+list+'"]:input').each(function(){
        if(this.localName == 'select' && this.name && this.multiple) {
            var $this = $(this);
            var val = '';
            if ($this.hasClass('selectpicker') && $this.val()) {
                $this.val().forEach(function (d) {
                    val = val + ';#' + d;
                })
            }
            else {
                $this.find(':selected').each(function () {
                    val = val + ';#' + this.value;
                });
            }
            if (val.length > 0)
                val = val.substr(2);
            obj[this.name] = val;
        }
        else if(this.type == 'checkbox'){
            obj[this.name] = $(this).is(':checked')? '1' : '0';
        }
        else if(this.name != ""){
            if($(this).hasClass('datepicker') && noNull(this.value) != '')
                obj[this.name] = moment(this.value).format("YYYY-MM-DDTHH:mm:ss") + 'Z';
            else
                obj[this.name] = this.value;
        }
    });
    return obj;
}

function populateFields(listName,item,$scope){
    $scope = typeof $scope == 'undefined' ?$(document):$scope;
    var keys = Object.keys(item);
    keys.forEach(function(key){
        var inputList = $scope.find('[list="'+listName+'"][name="'+key+'"]');
        inputList.each(function(){
            var input = $(this);
            var tag = input.prop("tagName");
            var value = item[key];
            if(value != null && typeof value == 'object' ){
                if(Array.isArray(value)){
                    var arr = [];
                    value.forEach(function(t){
                        if(tag == 'SPAN' || tag == 'DIV') {
                            if(typeof t.lookupValue != 'undefined')
                                arr.push(' ' + t.lookupValue);
                            else
                                arr.push(' '+t);
                        }
                        else {
                            if(typeof t.lookupValue != 'undefined')
                                arr.push(t.lookupId + ';#' + t.lookupValue);
                            else
                                arr.push(t);

                        }
                    })
                    value = arr
                }
                else if(typeof value.getMonth === 'function'){
                    //do nothing
                }
                else {
                    if(tag == 'SPAN' || tag == 'DIV')
                        value = value.lookupValue;
                    else
                        value = value.lookupId + ';#' + value.lookupValue;
                }
            }
            if(tag == 'INPUT' || tag == 'TEXTAREA') {
                if (input.hasClass('datepicker')) {
                    input.datepicker('update',value);
                    //   input.val(formatDisplayShortDate(item[key]));
                }
                else if(Array.isArray(value)){//Updated to join 12072016 - SL
                    input.val(value.join(';#'));
                }
                else
                    input.val(value);
            }
            else if(tag == 'SELECT') {
                //May not be needed  Multiple Lookup columns work properly
                //if(input.attr('multiple')){
                //    input.val(value);
                //}
                //else
                input.val(value);
            }
            else if(tag == 'SPAN' || tag == 'DIV'){
                if( value instanceof Date){
                    value = moment(value).format('YYYY-MM-DD')
                }
                input.text(noNull(value));
            }
        })

    })
    if($scope.find('.selectpicker').selectpicker)
        $scope.find('.selectpicker').selectpicker('refresh');
}


var myGroups = [];
function isInGroup(group){
    var retVal = false;
    if(myGroups.length == 0) {
        $().SPServices({
            operation: "GetGroupCollectionFromUser",
            userLoginName: $().SPServices.SPGetCurrentUser(),
            async: false,
            completefunc: function (xData, Status) {
                var groups = $(xData.responseXML);
                if (groups.find("Group[Name='" + group + "']").length == 1) {
                    retVal = true;
                }
                groups.find("Group[Name]").each(function () {
                    myGroups.push(this.attributes[1].nodeValue);
                })
            }
        });
    }
    else{
        return myGroups.indexOf(group)>-1;
    }
    return retVal;
}
function getMyGroups(){
    if(myGroups.length == 0) {
        isInGroup('test');//causes groups to refresh
    }
    return myGroups;
}

function getUsersOperator(cb,prompt){
    prompt = typeof prompt == 'undefined' ? true:false;
    $().SPServices({
        operation: "GetGroupCollectionFromUser",
        userLoginName: $().SPServices.SPGetCurrentUser(),
        async: true,
        completefunc: function(xData, Status) {
            var resp = $(xData.responseXML);
            // CR273 and 274.  Hey-you requirement from MR to also show the region selector for 2 other groups
            if(resp.find("Group[Name^='UASTS Operators Dev']").length != 0){
                cb("Dev");
            } else if (resp.find("Group[Name^='UASTS FAA RO']").length != 0)
            {
                cb("UASTSRO");
            } else if (resp.find("Group[Name^='UASTS Administrators']").length != 0)
            {
                cb("UASTSADMIN");
            } // CR273 and 274.  Hey-you requirement from MR to also show the region selector for 2 other groups
            else{
                var proponents = resp.find("Group[Name^='UASTS Operator']");
                if(proponents.length == 0) {
                    cb(null);
                }
                if(proponents.length == 1) {
                    prop = proponents.attr('Name').substr('UASTS Operator '.length).trim();
                    cb(prop)
                }
                else if(prompt){
                    var select = $('<select></select>')
                    proponents.each(function(){
                        var opt = $(this).attr('Name').substr('UASTS Operator '.length)
                        select.append('<option value="'+opt+'">'+opt+'</option>');
                    })

                    $("#dialog-confirm").html(select).find('select').selectpicker();

                    // Define the Dialog and its properties.
                    $("#dialog-confirm").dialog({
                        resizable : false,
                        modal : true,
                        title : "Choose Operator Role",
                        height : 250,
                        width : 400,
                        buttons : {
                            "OK" : function() {
                                $(this).dialog('close');
                                if (cb)
                                    cb($(this).find('select').val().trim());
                            }
                        }
                    });
                }
                else{
                    var arr = [];
                    proponents.each(function(){
                        arr.push($(this).attr('Name').substr('UASTS Operator '.length).trim());
                    });
                    cb(arr);
                }
            }
        }
    });
}

function fixPerms(listName){
    $().SPServices({
        operation: "GetListItems",
        async: false,
        listName: listName,
        CAMLViewFields: "<ViewFields><FieldRef Name='Title' /></ViewFields>",
        completefunc: function (xData, Status) {
            $(xData.responseXML).SPFilterNode("z:row").each(function() {
                var url = $(this).attr("ows_FileRef") ;
                url = url.substring(url.indexOf('#')+1)
                startWorkflow('https://ksn2.faa.gov/'+url)
            });
        }
    });
}
function startWorkflow(item){
    $().SPServices({
        operation: "StartWorkflow",
        item: item,
        templateId: '{29accaa9-ca1e-4da8-be7e-ec0d7da05208}',
        workflowParameters:  "<Data />",
        async: true,
        completefunc: function () {

        }

    });
    $().SPServices({
        operation: "StartWorkflow",
        item: item,
        templateId: '{26215bbb-86ef-49a0-b9d1-e752e803016d}',
        workflowParameters:  "<Data />",
        async: true,
        completefunc: function () {

        }

    });

}

function detectLocalOffset() {
    var url = $().SPServices.SPGetCurrentSite() + "/_layouts/regionalsetng.aspx?Type=User";

    $.get(url, function(data) {
        $(data).find("select[name$='LCID'] option:selected").each(function() {
            lcid = $(this).attr("value");
            cultureInfo = $(this).text();
        });
        $(data).find("select[name$='TimeZone'] option:selected").each(function() {
            timeZone = $(this).text();
        });
    });
}

function asyncFalse(){
    $().SPServices.defaults.async = false;
}
function asyncTrue(){
    $().SPServices.defaults.async = true;
}


var loadingCount = 0;
function loading(cb) {
    loadingCount++;
    console.log('loading:' + loadingCount);
    document.body.style.cursor = 'wait';
    ajaxindicatorstart();
    if(cb)
        setTimeout(cb,100);
}
function loadingDone(event) {
    if(typeof resetTimeout != 'undefined') resetTimeout();
    if (loadingCount == 0) {
        console.log('Loading Count is off');
        loadingCount = 1;// to keep the count right
    }
    if (--loadingCount == 0) {
        document.body.style.cursor = 'default';
        console.log('loading Done:' + loadingCount);
        ajaxindicatorstop();
    } else {
        console.log('Loading Count:' + loadingCount);
    }
    if(event){
        $(document).trigger(event)
    }
}

function noNull(str) {
    if(str === 0 || str == '0')
        return 0;
    return str ? str : ''
}

function ajaxindicatorinit(text) {
    if (jQuery('body').find('#resultLoading').attr('id') != 'resultLoading') {
        jQuery('body').append(
            '<div id="resultLoading" style="display:none"><div><img src="ajax-loader.gif"><div>' + text
            + '</div></div><div class="bg"></div></div>');
    }

    if($('#dialog-confirm').length == 0)
        $('body').append('<div id="dialog-confirm"></div>');
    if($('#dialog-error').length == 0)
        $('body').append('<div id="dialog-error"></div>');

    jQuery('#resultLoading').css({
        'width' : '100%',
        'height' : '100%',
        'position' : 'fixed',
        'z-index' : '10000000',
        'top' : '0',
        'left' : '0',
        'right' : '0',
        'bottom' : '0',
        'margin' : 'auto'
    });

    jQuery('#resultLoading .bg').css({
        'background' : '#000000',
        'opacity' : '0.7',
        'width' : '100%',
        'height' : '100%',
        'position' : 'absolute',
        'top' : '0'
    });

    jQuery('#resultLoading>div:first').css({
        'width' : '250px',
        'height' : '75px',
        'text-align' : 'center',
        'position' : 'fixed',
        'top' : '0',
        'left' : '0',
        'right' : '0',
        'bottom' : '0',
        'margin' : 'auto',
        'font-size' : '16px',
        'z-index' : '10',
        'color' : '#ffffff'

    });

    jQuery('#resultLoading .bg').height('100%');
    jQuery('#resultLoading').hide();
}

function ajaxindicatorstart() {
    jQuery('#resultLoading').show();//.fadeIn(300);
    jQuery('body').css('cursor', 'wait');
}
function ajaxindicatorstop() {
    jQuery('#resultLoading .bg').height('100%');
    jQuery('#resultLoading').fadeOut(300);
    jQuery('body').css('cursor', 'default');
}

function convertToFlightCentric(){
    var missionFields = COAMISSION_FIELDS.substring(0,COAMISSION_FIELDS.length-'</ViewFields>'.length)+'<FieldRef Name="Status" /></ViewFields>';
    GetListItemsJson({
        listName : "COAMission",
        CAMLViewFields : '<viewFields>' + COAMISSION_FIELDS + '</viewFields>'
    },function(){
        var missions = this.data;
        var missionList = {};
        for (var i = missions.length - 1; i >= 0; i--) {
            var mission = missions[i];
            missionList[mission.ID] = mission;
        }
        //var coaFields = COAFLIGHT_FIELDS.substring(0,COAFLIGHT_FIELDS.length-'</ViewFields>'.length)+'<FieldRef Name="FK_COA"/><FieldRef Name="FK_ResearchArea"/><FieldRef Name="FK_DetailedResearch"/></ViewFields>';
        GetListItemsJson({
            listName : "COAFlight",
            async : true,
            CAMLViewFields : '<viewFields>' + coaFields + '</viewFields>'
        },function() {
            var flights = this.data;
            flights.forEach(function(f){
                var mId = f.FK_COAMission.lookupId;
                var m = missionList[mId];
                //if(f.FK_COAMission  && !f.FK_COA) {
                //    if (m && m.FK_COA)
                //        saveListItem('COAFlight', {
                //            ID: f.ID,
                //            FK_COA: m.FK_COA.lookupId + ';#' + m.FK_COA.lookupValue
                //        }, null);
                //}
                //if(!f.FK_ResearchArea && m.FK_ResearchArea){
                //    saveListItem('COAFlight', {
                //        ID: f.ID,
                //        FK_ResearchArea: m.FK_ResearchArea,
                //        FK_DetailedResearch: m.FK_DetailedResearch
                //    }, null);
                //}
                if(m.Status == 'Submitted' && f.Status != 'Submitted'){
                    saveListItem('COAFlight', {
                        ID: f.ID,
                        Status: 'Submitted'
                    }, null);
                }
            });
        });
    });
}