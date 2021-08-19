//
// let overviewPagesInfo = {
//     "by_date" : {
//         "logo": "images/overviews/clock.png",
//         // "logo": "https://i.imgur.com/NmWKXcH.png",
//         "title": "New Releases",
//         "desc": "Newly added extensions to the G-ExtensionStore"
//     },
//     "by_rating" : {
//         "logo": "images/overviews/star.png",
//         // "logo": "https://i.imgur.com/vTpk1l6.png",
//         "title": "Most Popular Extensions",
//         "desc": "Extensions sorted by highest ratings"
//     },
//     "by_category" : {
//         "logo": "images/overviews/idea.png",
//         // "logo": "https://i.imgur.com/ubvaGgT.png",
//         "title": "Categories",
//         "desc": ""
//     },
//     "installed" : {
//         "logo": "images/overviews/success.png",
//         // "logo": "https://i.imgur.com/ubvaGgT.png",
//         "title": "Extensions you have already installed",
//         "desc": ""
//     },
//     "search" : {
//         "logo": "images/overviews/search.png",
//         // "logo": "https://i.imgur.com/ubvaGgT.png",
//         "title": "Categories",
//         "desc": ""
//     }
// };
//
// function setOverview(overviewName) {
//     let overview = overviewPagesInfo[overviewName];
//
//     $("#logo").empty();
//     $('#logo').prepend(`<img src="${overview["logo"]}" />`);
//
//     $("#info_title_container").html(overview["title"]);
//     $("#info_desc_container").html(overview["desc"]);
//
//     $("#content_title").html(overview["title"]);
// }
//
// function setCategory(badge, title, desc) {
//     $("#logo").empty();
//     $('#logo').prepend(`<img src="${badge}" />`);
//
//     $("#info_title_container").html(title);
//     $("#info_desc_container").html(desc);
//

function setHeading(badge, title, desc) {
    $("#logo").empty();
    $('#logo').prepend(`<img src="${badge}" />`);

    $("#info_title_container").html(title);
    $("#info_desc_container").html(desc);
}

function setContentTitle(text) {
    $("#content_title").html(text);
}



//
// function setExtensionView(icon, title, desc) {
//     $("#logo").empty();
//     $('#logo').prepend(`<img src="${icon}" />`);
//
//     $("#info_title_container").html(title);
//     $("#info_desc_container").html(desc);
//
//     $("#content_title").html(title);
// }
//
// // $(window).on('load', function () {
// //     for (let overviewName in overviewPagesInfo) {
// //         $(`#overview_${overviewName}`).click( function(e) {e.preventDefault(); setOverview(overviewName)});
// //     }
// //
// // });
