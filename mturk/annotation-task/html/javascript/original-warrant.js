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
 * Checks whether all stance-related radio groups are checked
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
 * Validates if an argument is fully annotated
 * @param reasonId argument
 * @returns {boolean} true if we have all required annotations, false otherwise
 */
function validateSingleAnnotatedArgument(reasonId) {
    // check whether the some groups have unfilled buttons
    // var familiarSelected = $("input[name=" + reasonId + "_familiarGroup]:checked").val();

    var warrantImpossible = $("input[name=" + reasonId + "_fixed_warrant_impossible]").is(":checked");
    // console.log("Warrant impossible: " + warrantImpossible);

    var warrantImpossibleWhy = $("input[name=" + reasonId + "_impossible_why]").val().length > 0;
    // console.log("Warrant impossible why: " + warrantImpossibleWhy);

    // var filledText = $("textarea[name=" + reasonId + "_opposingStance_warrant]").val().length > 0;
    var filledText = $("input[name=" + reasonId + "_fixed_warrant]").val().length > 0;
    // console.log("Filled text: " + filledText);

    var result = ((warrantImpossible && warrantImpossibleWhy) || (!warrantImpossible && filledText));

    return result;
}

/**
 * Updates the sanity check box
 * @param reasonId
 */
function updateSanityCheckBox(reasonId) {
    var container = $("#" + reasonId + "_fixed_warrant_sanity");
    var rephrasedText = $("#" + reasonId + "_fixed_warrant");

    container.text(rephrasedText.val());
}

$(document).ready(function () {
    // wait for any check-box button to change
    $("input:checkbox").change(function () {
        validateFormAndUpdateCollectedResults();

        // disable/enable the text field
        var reasonId = $(this).data("reasonid");
        var textboxWarrant = $("#" + reasonId + "_fixed_warrant");
        var textboxWhy = $("#" + reasonId + "_impossible_why_div");
        if ($(this).is(":checked")) {
            $(textboxWarrant).prop('disabled', true);
            $(textboxWhy).show(0);
        } else {
            $(textboxWarrant).prop('disabled', false);
            $(textboxWhy).hide();
        }
    });

    // disable hitting enter to submit the form
    $('form input:text').on('keyup', function (e) {
        validateFormAndUpdateCollectedResults();

        var reasonId = $(this).data("reasonid");
        updateSanityCheckBox(reasonId);
        return e.which !== 13;
    });

    // disable hitting enter to submit the form
    $('form input:text').bind('input', function (e) {
        validateFormAndUpdateCollectedResults();
        return e.which !== 13;
    });

    // show/hide examples
    $('#ex_toggle_examples').click(function(){
        $('#examples').toggle(400);
    });

});
