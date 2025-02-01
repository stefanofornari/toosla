const CONFIG_CLOCK_TIMEZONE = "toosla.clock.timezone";

class Clock {

    currentTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;  // default timzone for time

    dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full' });
    timeFormat = new Intl.DateTimeFormat('en-GB', { timeStyle: 'medium', timeZone: 'Europe/Rome' });  // TODO: set a better default

    #taskID = null;


    clocks() {
        return document.querySelectorAll(".clock");
    };

    currentDates(){
        return document.querySelectorAll(".current-date");
    }

    timeZone(tz) {
        console.info("Setting timezone to", tz)
        this.currentTimezone = tz;
        this.timeFormat = new Intl.DateTimeFormat('en-GB', { timeStyle: 'medium', timeZone: tz });
        this.dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full', timeZone: tz  })
    }

    updateTime() {
        const nb = [
            "inset 0 0 0 var(--size) var(--color), 0px var(--size) var(--color), 0px calc(var(--size) * 2) var(--color), 0px calc(var(--size) * 3) var(--color), 0px calc(var(--size) * 4) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) rgb(0 0 0 / 0),calc(var(--size)*2) var(--size) var(--color), calc(var(--size)*2) 0 var(--color), calc(var(--size)*2) calc(var(--size) * 2) var(--color), calc(var(--size)*2) calc(var(--size) * 3) var(--color), calc(var(--size)*2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color), var(--size) 0px var(--color), calc(var(--size) * 2) 0px var(--color), calc(var(--size) * 3) 0px var(--color), calc(var(--size) * 3) var(--size) var(--color), 0 calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), 0 calc(var(--size) * 3) var(--color), 0 calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),0px calc(var(--size) * 4) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color),var(--size) calc(var(--size) * 2) var(--color),0 calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),calc(var(--size)* 3) 0 var(--color), calc(var(--size)* 3) var(--size) var(--color),  calc(var(--size)* 3) calc(var(--size) * 2) var(--color), calc(var(--size)* 2) calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), 0 calc(var(--size) * 2) var(--color), 0 var(--size) var(--color), calc(var(--size)* 3) calc(var(--size) * 3) var(--color), calc(var(--size)* 3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color), 0px var(--size) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), 0 calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), 0 calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),0px var(--size) var(--color), 0px calc(var(--size) * 2) var(--color), 0px calc(var(--size) * 3) var(--color), 0px calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size)*2) calc(var(--size) * 4) var(--color), calc(var(--size)*3) calc(var(--size) * 3) var(--color), calc(var(--size)*3) calc(var(--size) * 2) var(--color), calc(var(--size)*2) calc(var(--size) * 2) var(--color), var(--size) calc(var(--size) * 2) var(--color), calc(var(--size)*3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 2) 0 var(--color), var(--size) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),0px var(--size) var(--color), 0px calc(var(--size) * 2) var(--color), 0px calc(var(--size) * 3) var(--color), 0px calc(var(--size) * 4) var(--color), var(--size) 0 var(--color), calc(var(--size) * 2) 0 var(--color), calc(var(--size) * 3) 0 var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 2) calc(var(--size) * 2) var(--color),var(--size) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color), var(--size) calc(var(--size) * 4) var(--color), calc(var(--size) * 2) calc(var(--size) * 4) var(--color)",
            "inset 0 0 0 var(--size) var(--color),calc(var(--size) * 3) 0 var(--color),calc(var(--size) * 2) 0 var(--color),var(--size) 0 var(--color),calc(var(--size) * 2) calc(var(--size) * 2) var(--color),var(--size) calc(var(--size) * 2) var(--color),0 calc(var(--size) * 2) var(--color),0 var(--size) var(--color), calc(var(--size) * 3) var(--size) var(--color), calc(var(--size) * 3) calc(var(--size) * 2) var(--color), calc(var(--size) * 3) calc(var(--size) * 3) var(--color), calc(var(--size) * 3) calc(var(--size) * 4) var(--color)"
          ];

        const today = new Date();
        const timeString = this.timeFormat.format(today);
        const time = timeString.split("").filter((v) => v !== ":");

        const todayDate = this.dateFormat.format(today);
        this.currentDates().forEach((currentDate) => {
            currentDate.innerHTML = todayDate;
        });

        this.clocks().forEach((clock) => {
            const divs = clock.querySelectorAll("div");
            time.forEach((t, i) => {
                if (t !== ":") {
                    for (let x = 0; x < divs.length; x++) {
                        const cell = divs[x].querySelector(`span:nth-child(${i + 1})`);
                        cell.style.boxShadow = nb[parseInt(t)];
                    }
                }
            });
        });
    }

    setup() {
        const self = this;

        //
        // initialize available timezones and add them to the timezone select
        // in settings
        //

        $("#clock .settings select.timezones").on("itemselect", function() {
            self.timeZone($(this).val());
        });

        try {
            const TZ = toosla.storage.getItem(CONFIG_CLOCK_TIMEZONE);
            if (TZ) {
                this.timeZone(TZ);
            }
        } catch (e) {
            console.error("Error reading TZ from local storage, using default");
        }

        self.updateTime();
        if (self.#taskID !== null) {
            clearInterval(self.#taskID);
        }
        self.#taskID = setInterval(() => self.updateTime(), 500);
    }

    settings(event) {
        console.debug("Clock settings action:", event);

        const select = $("#clock .settings select.timezones");
        if (event === 'open') {
            $('#clock .settings').removeAttr('hidden');

            if (Metro.getPlugin(select, "select")) {
                select.metro('select', 'destroy');
            }
            for (const tz of Intl.supportedValuesOf('timeZone')) {
                select.append($("<option>").attr("value", tz).html(tz))
            }
            Metro.makePlugin(select, "select");
        } else if (event === 'close') {
            $('#clock .settings').attr('hidden', 'true');
        } else if (event === 'load') {
            try {
                const TZ = toosla.storage.getItem(CONFIG_CLOCK_TIMEZONE);
                if (TZ) {
                    this.timeZone(TZ);
                }
            } catch (e) {
                console.error("Error reading TZ from local storage, using default");
            }
        } else if (event === 'save') {
            const TZ = Metro.getPlugin(select, "select").val();
            this.timeZone(TZ);
            toosla.storage.setItem(CONFIG_CLOCK_TIMEZONE, TZ);
        }
    }
}

const CLOCK = new Clock();
toosla.modules['clock'] = CLOCK;
$(document).ready(() => {
    try {
        CLOCK.setup();
    } catch (e) {
        console.error(e);
    }
});

