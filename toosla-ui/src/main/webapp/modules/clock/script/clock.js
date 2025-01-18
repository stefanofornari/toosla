class Clock {

    dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full' });
    timeFormat = new Intl.DateTimeFormat('en-GB', { timeStyle: 'medium', timeZone: 'Europe/Rome' });

    clocks() {
        return document.querySelectorAll(".clock");
    };

    currentDates(){
        return document.querySelectorAll(".current-date");
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
            today.toLocaleString();
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

        self.updateTime();
        setInterval(() => self.updateTime(), 500);
    }
}

new Clock().setup();
toosla.modules['clock'] = new Clock();
