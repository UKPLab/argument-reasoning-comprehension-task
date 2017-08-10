/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Checks whether all stance-related radio groups are checked and whether
 * there is either a selection of segments done or a checkbox saying that there
 * are no explicit segments
 */
function validateFormAndUpdateCollectedResults() {
    // make sure all arguments have selected stance from the radio group
    var allOk = true;

    // check where all have stance to 0 or 1, and if so, whether the
    // sentences were highlighted or checked implicitness
    $('div.argument').each(function () {
        allOk &= validateSingleAnnotatedArgument($(this).attr("id"));
    });

    // update the submit button
    $("#submitButton").prop("disabled", !allOk);

}

/**
 * Validates if an argument is fully annotated, updates the global container with annotations
 * @param reasonId argument
 * @returns {boolean} true if we have all required annotations, false otherwise
 */
function validateSingleAnnotatedArgument(reasonId) {
    // check whether the some groups have unfilled buttons
    // var familiarSelected = $("input[name=" + reasonId + "_familiarGroup]:checked").val();

    var warrantImpossible = $("input[name=" + reasonId + "_no_reason]").is(":checked");

    var filledText = $("textarea[name=" + reasonId + "_rephrasedReason]").val().length > 0;

    result = warrantImpossible || filledText;

    return result;
}

/**
 * Updates the sanity check box
 * @param reasonId
 */
function updateSanityCheckBox(reasonId) {
    var container = $("#" + reasonId + "_sanityContainer");
    var rephrasedText = $("#" + reasonId + "_rephrasedReason");

    container.text(rephrasedText.val());
}

$(document).ready(function () {
    // wait for any radio button to change
    $("input:radio").change(function () {
        validateFormAndUpdateCollectedResults();
    });

    // wait for any check-box button to change
    $("input:checkbox").change(function () {
        validateFormAndUpdateCollectedResults();

        // disable/enable the text field
        var reasonId = $(this).data("reasonid");
        var textbox = $("#" + reasonId + "_rephrasedReason");
        if ($(this).is(":checked")) {
            $(textbox).prop('disabled', true);
        } else {
            $(textbox).prop('disabled', false);
        }
    });

    // counter for characters
    // $(".counter").keyup(function () {
    //     var element = $("#" + $(this).attr("id") + "_length");
    //     var maxCharacters = element.attr("title");
        // trim if longer
        // if ($(this).val().length >= maxCharacters) {
        //     $(this).val($(this).val().substr(0, maxCharacters));
        // }
        // element.text($(this).val().length);
    // });

    // disable hitting enter to submit the form
    $('form textarea').on('keyup', function (e) {
        var reasonId = $(this).data("reasonid");
        updateSanityCheckBox(reasonId);

        validateFormAndUpdateCollectedResults();
        return e.which !== 13;
    });

    // disable hitting enter to submit the form
    $('form input:text').bind('input', function () {
        console.log("here");
        validateFormAndUpdateCollectedResults();
    });

    // show/hide examples
    $('#ex_toggle_examples').click(function(){
        $('#examples').toggle(400);
    });

});
