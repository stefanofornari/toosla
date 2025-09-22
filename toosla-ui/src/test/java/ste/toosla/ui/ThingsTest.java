/*
 * Toosla.me
 * ---------
 *
 * Copyright (C) 2025 Stefano Fornari. Licensed under the
 * EUPL-1.2 or later (see LICENSE).
 *
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ste.toosla.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.assertj.core.api.BDDAssertions.then;
import org.json.JSONArray;
import org.junit.Test;
import ste.xtest.json.api.JSONAssertions;

/**
 *
 */
public class ThingsTest extends TooslaTestBase {
    @Test
    public void things_displayed_on_page() {
        then(visible("#things")).isTrue();
        then((boolean)exec("toosla.getModuleController('things') !== undefined;")).isTrue();
        //
        // starting in view mode
        //
        then(visible("#things div.list-view")).isTrue();
        then(visible("#things div.edit-view")).isFalse();
        then(visible("#things .card-header .btn-done")).isFalse();
        then(visible("#things .card-header .btn-cancel")).isFalse();
    }

    @Test
    public void available_toosla_buttons() {
        then(visible("#things .application-action-bar button.button.toosla-btn-fullscreen")).isTrue();
    }

    @Test
    public void click_on_edit_shows_the_edit_view() throws Exception {
        exec("$('#things tr')[1].children[1].click()");
        then(visible("#things div.list-view")).isFalse();
        then(visible("#things .card-header .btn-done")).isTrue();
        then(visible("#things .card-header .btn-cancel")).isTrue();
        then(visible("#things div.edit-view")).isTrue();

        exec("$('things .btn-done').click();");
        then(visible("#things div.list-view")).isTrue();
        then(visible("#things .card-header .btn-done")).isFalse();
        then(visible("#things .card-header .btn-cancel")).isFalse();
        then(visible("#things div.edit-view")).isFalse();
    }

    @Test
    public void click_on_done_or_cancel_hides_edit_mode() throws Exception {
        exec("$('#things tr')[1].children[1].click()");  // edit mode

        exec("$('#things .btn-done').click();");
        then(visible("#things div.list-view")).isTrue();
        then(visible("#things .card-header .btn-done")).isFalse();
        then(visible("#things .card-header .btn-cancel")).isFalse();
        then(visible("#things div.edit-view")).isFalse();

        exec("$('#things tr')[1].children[1].click()");  // edit mode

        exec("$('#things .btn-cancel').click();");
        then(visible("#things div.list-view")).isTrue();
        then(visible("#things .card-header .btn-done")).isFalse();
        then(visible("#things .card-header .btn-cancel")).isFalse();
        then(visible("#things div.edit-view")).isFalse();
    }

    @Test
    public void create_default_thing_element() throws Exception {
        //
        // assuming no items have been stored yet
        //
        then((String)exec("$('#things tr').children('td')[1].innerText")).isEqualToIgnoringNewLines("Tap to edit your first thing");
        then((String)exec("$('#things tr').children('td')[2].innerText")).isEqualToIgnoringNewLines(
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );
    }

    @Test
    public void edit_shows_current_thing_values() throws Exception {
        exec("$('#things tr')[1].children[1].click()");  // edit mode

        final String TODAY = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyddMMMM"));
        then((Boolean)exec("$('#things .edit-view input[name=status]')[0].checked")).isFalse();
        then(val("#things .edit-view textarea")).isEqualTo("Tap to edit your first thing");
        then(text("#things .edit-view .year")).isEqualTo(TODAY.substring(0, 4));
        then(Integer.parseInt(text("#things .edit-view .day"))).isEqualTo(Integer.parseInt(TODAY.substring(4, 6)));
        then(text("#things .edit-view .month")).isEqualTo(TODAY.substring(6));

        //
        // another thing
        //
        exec("""
            $('#things .btn-cancel').click();
            const t = angular.element($('#things')).controller('things').things[0];
            t.text = 'another text';
            t.when = '2011-10-05';
            t.status = "done";
            $('#things tr')[1].children[1].click();
        """);
        then(exec("$('#things .edit-view input[name=status]:checked').length > 0"));
        then(val("#things .edit-view textarea")).isEqualTo("another text");
        then(text("#things .edit-view .year")).isEqualTo("2011");
        then(text("#things .edit-view .day")).isEqualTo("5");
        then(text("#things .edit-view .month")).isEqualTo("October");
    }

    @Test
    public void done_saves_changes() {
        //
        // note that we do not care about the remote storage here...
        //
        exec("""
            $('#things tr')[1].children[1].click();  // edit mode

            angular.element($('#things .edit-view input[name=status]'))[0].click();
            angular.element($('#things textarea')).val('hello\\nworld');
            $('#things textarea').trigger('input');
            Metro.getPlugin($('#things .edit-view input[name=when]')[0], "datepicker").val('2020-01-01T12:38:09.589Z');

            $('#things .btn-done').click();  // done
        """);

        JSONArray things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        JSONAssertions.then(things.getJSONObject(0))
            .containsEntry("status", "done")
            .containsEntry("text", "hello\nworld")
            .containsEntry("when", "2020-01-01");

        //
        // items stored in local storage
        //
        things = new JSONArray((String)exec("toosla.storage.getItem('toosla.things.things');"));
        JSONAssertions.then(things).hasSize(1);
        then(things.getJSONObject(0).getJSONArray("things")).isEmpty();
        JSONAssertions.then(things.getJSONObject(0))
            .containsEntry("status", "done")
            .containsEntry("text", "hello\nworld")
            .containsEntry("when", "2020-01-01");
    }

    @Test
    public void cancel_discard_changes() {
        exec("""
            $('#things tr')[1].children[1].click();  // edit mode

            angular.element($('#things .edit-view input[name=status]'))[0].click();
            angular.element($('#things textarea')).val('hello\\nworld');
            $('#things textarea').trigger('input');
            Metro.getPlugin($('#things .edit-view input[name=when]')[0], "datepicker").val('2020-01-01T12:38:09.589Z');

            $('#things .btn-cancel').click();  // cancel
        """);

        JSONArray things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        then(things.getJSONObject(0).getJSONArray("things")).isEmpty();
        JSONAssertions.then(things.getJSONObject(0))
            .containsEntry("status", "active")
            .containsEntry("text", "Tap to edit your first thing")
            .containsEntry("when", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    @Test
    public void load_things_at_start() throws Exception {
        final String THINGS = """
        [
            {
                "status": "done",
                "text": "first thing",
                "when": "2010-01-01",
                "things": []
            },
            {
                "status": "active",
                "text": "next thing",
                "when": "2020-02-02",
                "things": []
            }
        ]
        """;

        exec("toosla.storage.setItem('toosla.things.things', `" + THINGS + "`);");

        loadPage(); // reload the page after saving the things
        exec("$('#things tr')[1].children[1].click();");

        then((Boolean)exec("$('#things .edit-view input[name=status]')[0].checked")).isTrue();
        then(val("#things .edit-view textarea")).isEqualTo("first thing");
        then(text("#things .edit-view .year")).isEqualTo("2010");
        then(text("#things .edit-view .day")).isEqualTo("1");
        then(text("#things .edit-view .month")).isEqualTo("January");

        click("#things .btn-cancel"); exec("$('#things tr')[2].children[1].click();");

        then((Boolean)exec("$('#things .edit-view input[name=status]')[0].checked")).isFalse();
        then(val("#things .edit-view textarea")).isEqualTo("next thing");
        then(text("#things .edit-view .year")).isEqualTo("2020");
        then(text("#things .edit-view .day")).isEqualTo("2");
        then(text("#things .edit-view .month")).isEqualTo("February");
    }

    @Test
    public void add_new_thing() throws Exception {
        click("#things .btn-add");

        then(visible("#things .edit-view")).isTrue();

        //
        // set some values and click done to add the thing to the array and in
        // the local storage
        //
        exec("""
            angular.element($('#things .edit-view input[name=status]'))[0].click();
            angular.element($('#things textarea')).val('hello\\nworld');
            $('#things textarea').trigger('input');
            Metro.getPlugin($('#things .edit-view input[name=when]')[0], "datepicker").val('2020-01-01T12:38:09.589Z');
        """);
        click("#things .btn-done");
        then((Boolean)exec("$('#things tr').length === 3;")).isTrue();
        JSONArray things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        JSONAssertions.then(things).hasSize(2);
        then(things.getJSONObject(1).getJSONArray("things")).isEmpty();
        JSONAssertions.then(things.getJSONObject(1))
            .containsEntry("status", "done")
            .containsEntry("text", "hello\nworld")
            .containsEntry("when", "2020-01-01");
    }

    @Test
    public void delete_a_thing_shows_confirmation_dialog() throws Exception {
        exec("$('#things .btn-bin')[0].click()");  // delete first element
        then(visible("#things dialog.delete-confirm")).isTrue();
        then(text("#things dialog .dialog-content").trim()).isEqualTo("Tap to edit your first thing");
    }

    @Test
    public void delete_or_cancel_a_thing() throws Exception {
        final String THINGS = """
        [
            {
                "status": "done",
                "text": "first thing",
                "when": "2010-01-01",
                "things": []
            },
            {
                "status": "active",
                "text": "next thing",
                "when": "2020-02-02",
                "things": []
            }
        ]
        """;

        exec("toosla.storage.setItem('toosla.things.things', `" + THINGS + "`);");

        loadPage(); // reload the page after saving the things

        exec("$('#things .btn-bin')[1].click()");  // delete second element
        click("#things .delete-confirm .btn-cancel"); // do not confirm
        then(visible("#things dialog.delete-confirm")).isFalse();

        then(exec("JSON.parse(toosla.storage.getItem('toosla.things.things')).length ")).isEqualTo(2);
        JSONArray things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        JSONAssertions.then(things).hasSize(2);

        exec("$('#things .btn-bin')[1].click()");  // delete second element
        click("#things .delete-confirm .btn-confirm"); // confirm
        then(visible("#things dialog.delete-confirm")).isFalse();

        then(exec("JSON.parse(toosla.storage.getItem('toosla.things.things')).length ")).isEqualTo(1);
        things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        JSONAssertions.then(things).hasSize(1);
        JSONAssertions.then(things.getJSONObject(0))
            .containsEntry("status", "done");

        exec("$('#things .btn-bin')[0].click()");  // delete second element
        click("#things .delete-confirm .btn-confirm"); // confirm

        then(exec("JSON.parse(toosla.storage.getItem('toosla.things.things')).length ")).isEqualTo(0);
    }

    @Test
    public void show_max_50_chars_of_the_first_line() throws Exception {
        final String THINGS = """
        [
            {
                "status": "done",
                "text": "first line\\\\nsecond line\\\\nthird line",
                "when": "2010-01-01",
                "things": []
            },
            {
                "status": "active",
                "text": "very long description in the first line shall be cut with ellips",
                "when": "2020-02-02",
                "things": []
            }
        ]
        """;
        exec("toosla.storage.setItem('toosla.things.things', `" + THINGS + "`);");
        loadPage(); // reload the page after saving the things

        then(exec("$('#things tr')[1].children[1].innerText.trim()"))
                .isEqualTo("first line");
        then(exec("$('#things tr')[2].children[1].innerText.trim()"))
                .isEqualTo("very long description in th…");
    }

    @Test
    public void reloads_things_on_storage_change() throws Exception {
        loadPage(); // start with empty storage

        final String THINGS = """
        [
            {
                "status": "active",
                "text": "new thing",
                "when": "2022-02-02",
                "things": []
            }
        ]
        """;

        exec("toosla.storage.setItem('toosla.things.things', `" + THINGS + "`);");

        waitUntilTrue("angular.element($('#things')).controller('things').things[0].text === 'new thing'");

        JSONArray things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        JSONAssertions.then(things).hasSize(1);
        JSONAssertions.then(things.getJSONObject(0))
            .containsEntry("text", "new thing");
    }

    @Test
    public void toogle_status() throws Exception {
        final String THINGS = """
        [
            {
                "status": "done",
                "text": "first line\\\\nsecond line\\\\nthird line",
                "when": "2010-01-01",
                "things": []
            },
            {
                "status": "active",
                "text": "very long description in the first line shall be cut with ellips",
                "when": "2020-02-02",
                "things": []
            }
        ]
        """;
        exec("toosla.storage.setItem('toosla.things.things', `" + THINGS + "`);");
        loadPage(); // reload the page after saving the things

        then((Boolean)exec("$('#things input[name=status]')[0].checked")).isTrue();
        then((Boolean)exec("$('#things input[name=status]')[1].checked")).isFalse();

        click("#things input[name=status]')[0]");
        click("#things input[name=status]')[1]");

        then((Boolean)exec("$('#things input[name=status]')[0].checked")).isTrue();
        then((Boolean)exec("$('#things input[name=status]')[1].checked")).isFalse();

        JSONArray things = new JSONArray((String)exec("angular.element($('#things')).controller('things').things"));
        JSONAssertions.then(things.getJSONObject(0)).containsEntry("status", "done");
        JSONAssertions.then(things.getJSONObject(1)).containsEntry("status", "active");
    }
}
