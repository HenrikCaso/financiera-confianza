document.addEventListener("DOMContentLoaded", () => {

    const items = document.querySelectorAll(".timeline-item");

    items.forEach((item, index) => {

        item.style.opacity = "0";

        setTimeout(() => {
            item.style.transition = "0.4s";
            item.style.opacity = "1";
        }, index * 150);

    });

});