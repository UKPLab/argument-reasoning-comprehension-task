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

function gup(name) {
    var regexS = "[\\?&amp;]" + name + "=([^&amp;#]*)";
    var regex = new RegExp(regexS);
    var tmpURL = window.location.href;
    var results = regex.exec(tmpURL);
    if (results == null)
        return "";
    else
        return results[1];
}

// deprecated?
function decode(strToDecode) {
    var encoded = strToDecode;
    return unescape(encoded.replace(/\+/g, " "));
}


//document.DElementById('pageFrame').src = decode(gup('url'));
document.getElementById('assignmentId').value = gup('assignmentId');


// this comes from original MTurk script
// Check if the worker is PREVIEWING the HIT or if they've ACCEPTED the HIT

if (gup('assignmentId') == "ASSIGNMENT_ID_NOT_AVAILABLE") {
    // If we're previewing, disable the button and give it a helpful message

    document.getElementById('submitButton').disabled = true;
    document.getElementById('submitButton').value = "You must ACCEPT the HIT before you can submit the results.";
} else {
    var form = document.getElementById('mturk_form');
    if (document.referrer && ( document.referrer.indexOf('workersandbox') != -1)) {
        form.action = "https://workersandbox.mturk.com/mturk/externalSubmit";
    }
}
