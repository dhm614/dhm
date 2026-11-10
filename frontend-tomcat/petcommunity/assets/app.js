(function () {
    var state = {
        apiBase: localStorage.getItem("pet_api_base") || "http://localhost:8082",
        authUser: readAuthUser(),
        loginRole: "USER",
        stores: [],
        serviceItems: [],
        schedules: [],
        scheduleStoreId: null,
        products: [],
        checkoutCart: [],
        adminUserFilterId: null,
        profileUserId: null,
        profileFollowed: false,
        nearbyStoreMap: null,
        nearbyStorePoint: null,
        storeCardHighlightTimer: null,
        storeLocationMap: null
    };

    var actions = {
        logout: logout,
        loadDashboard: loadDashboard,
        loadNotifications: loadNotifications,
        markAllNotifications: markAllNotifications,
        loadPosts: loadPosts,
        loadTopics: loadTopics,
        searchPosts: searchPosts,
        selectPostTopic: selectPostTopic,
        selectVideoTopic: selectVideoTopic,
        uploadPostCover: uploadPostCover,
        loadPets: loadPets,
        uploadPetAvatar: uploadPetAvatar,
        loadStores: loadStores,
        loadNearbyStores: loadNearbyStores,
        locateNearbyStores: locateNearbyStores,
        locateStoreLocation: locateStoreLocation,
        toggleScheduleCalendar: toggleScheduleCalendar,
        uploadStoreCover: uploadStoreCover,
        loadProducts: loadProducts,
        uploadServiceItemCover: uploadServiceItemCover,
        uploadProductCover: uploadProductCover,
        uploadMerchantProductCover: uploadMerchantProductCover,
        uploadMerchantApplicationAttachment: uploadMerchantApplicationAttachment,
        loadCart: loadCart,
        goCheckout: goCheckout,
        goShop: goShop,
        toggleOrders: toggleOrders,
        loadProfile: loadProfile,
        toggleProfileFollow: toggleProfileFollow,
        loadMyAppointments: loadMyAppointments,
        loadMerchantAppointments: loadMerchantAppointments,
        loadAdminData: loadAdminData,
        loadAllAdminUsers: loadAllAdminUsers,
        loadAdminTopics: loadAdminTopics,
        loadAdminComments: openAdminComments,
        loadReports: loadReports,
        loadSensitiveWords: loadSensitiveWords
    };

    document.addEventListener("DOMContentLoaded", init);

    function init() {
        var page = document.body.getAttribute("data-page") || "login";
        bindApiInput();
        bindGlobalActions();

        if (page === "login") {
            initLoginPage();
            return;
        }
        if (page === "register") {
            initRegisterPage();
            return;
        }
        if (!requireLogin()) {
            return;
        }

        var navPage = page === "checkout" ? "shop" : page;
        if (page === "community-publish" || page === "community-topic-apply" || page === "post-detail") {
            navPage = "community";
        }
        if (page === "video-publish" || page === "video-detail") {
            navPage = "videos";
        }
        if (page === "merchant-apply" || page === "store-register") {
            navPage = "profile";
        }
        if (page === "my-posts" || page === "my-videos" || page === "my-appointments") {
            navPage = "profile";
        }
        if (page === "merchant-appointments") {
            navPage = "services";
        }
        if (page === "admin-audit-detail") {
            navPage = "admin";
        }
        initHeader(navPage);
        if (page === "dashboard" && !requireAdmin()) {
            return;
        }
        if ((page === "admin" || page === "admin-audit-detail") && !requireAdmin()) {
            return;
        }
        if ((page === "merchant-product-create" || page === "merchant-products") && !requireMerchant()) {
            return;
        }
        if (page === "merchant-appointments" && !requireMerchantServiceManager()) {
            return;
        }
        if (page === "store-register" && !requireMerchantStoreRegistration()) {
            return;
        }

        if (page === "home") {
            initHomePage();
        } else if (page === "dashboard") {
            initDashboardPage();
        } else if (page === "community") {
            initCommunityPage();
        } else if (page === "community-publish") {
            initCommunityPublishPage();
        } else if (page === "community-topic-apply") {
            initCommunityTopicApplyPage();
        } else if (page === "post-detail") {
            initPostDetailPage();
        } else if (page === "videos") {
            initVideosPage();
        } else if (page === "video-publish") {
            initVideoPublishPage();
        } else if (page === "video-detail") {
            initVideoDetailPage();
        } else if (page === "pets") {
            initPetsPage();
        } else if (page === "services") {
            initServicesPage();
        } else if (page === "shop") {
            initShopPage();
        } else if (page === "checkout") {
            initCheckoutPage();
        } else if (page === "profile") {
            initProfilePage();
        } else if (page === "my-posts") {
            initMyPostsPage();
        } else if (page === "my-videos") {
            initMyVideosPage();
        } else if (page === "my-appointments") {
            initMyAppointmentsPage();
        } else if (page === "merchant-appointments") {
            initMerchantAppointmentsPage();
        } else if (page === "merchant-apply") {
            initMerchantApplyPage();
        } else if (page === "store-register") {
            initStoreRegisterPage();
        } else if (page === "admin") {
            initAdminPage();
        } else if (page === "admin-audit-detail") {
            initAdminAuditDetailPage();
        } else if (page === "merchant-product-create") {
            initMerchantProductCreatePage();
        } else if (page === "merchant-products") {
            initMerchantProductsPage();
        }
    }

    function initLoginPage() {
        clearAuthUser();
        saveApiBase("http://localhost:8082");
        bindLoginRoleTabs();
        bindSubmit("loginForm", login);
    }

    function initRegisterPage() {
        bindSubmit("registerForm", register);
    }

    function initHomePage() {
        loadHomeHighlights();
    }

    function initDashboardPage() {
        loadDashboard();
    }

    function initCommunityPage() {
        loadPosts();
    }

    function initCommunityPublishPage() {
        bindSubmit("postForm", createPost);
        loadSelectableTopics();
    }

    function initCommunityTopicApplyPage() {
        bindSubmit("topicForm", createTopic);
        loadTopics();
    }

    function initPostDetailPage() {
        loadPostDetail();
    }

    function initVideosPage() {
        loadVideos();
    }

    function initVideoPublishPage() {
        bindSubmit("videoForm", createShortVideo);
        loadSelectableVideoTopics();
    }

    function initVideoDetailPage() {
        bindSubmit("videoCommentForm", submitVideoComment);
        loadVideoDetail();
    }

    function initPetsPage() {
        bindSubmit("petForm", createPet);
        loadPets();
    }

    function initServicesPage() {
        applyMerchantVisibility();
        if (canManageMerchantServices()) {
            bindSubmit("serviceItemForm", createServiceItem);
            bindSubmit("scheduleForm", createSchedule);
        }
        bindSubmit("appointmentForm", createAppointment);
        loadStores();
        initNearbyStoreMap();
    }

    function initShopPage() {
        var merchantActions = byId("merchantShopActions");
        if (merchantActions) {
            merchantActions.hidden = !isApprovedMerchant();
        }
        loadProducts();
        loadCart();
    }

    function initCheckoutPage() {
        bindSubmit("checkoutForm", submitCheckout);
        loadCheckout();
    }

    function initProfilePage() {
        var input = byId("profileUserId");
        var targetUserId = new URLSearchParams(window.location.search).get("userId") || userId();
        if (input) {
            input.value = targetUserId;
        }
        loadProfile();
    }

    function initMyPostsPage() {
        loadMyPosts();
    }

    function initMyVideosPage() {
        loadMyVideos();
    }

    function initMyAppointmentsPage() {
        loadMyAppointments();
    }

    function initMerchantAppointmentsPage() {
        var selector = byId("merchantAppointmentStoreId");
        if (selector) {
            selector.onchange = loadMerchantAppointments;
        }
        loadMerchantAppointmentStores();
    }

    function initMerchantApplyPage() {
        bindSubmit("merchantApplicationForm", submitMerchantApplication);
        loadMerchantApplication();
    }

    function initStoreRegisterPage() {
        bindSubmit("storeForm", createStore);
        initStoreLocationMap();
    }

    function initAdminPage() {
        bindSubmit("adminTopicForm", createAdminTopic);
        bindSubmit("sensitiveWordForm", createSensitiveWord);
        bindSubmit("adminUserSearchForm", searchAdminUsers);
        loadAdminData();
        loadAdminTopics();
    }

    function initMerchantProductCreatePage() {
        bindSubmit("merchantProductCreateForm", submitMerchantProductCreate);
    }

    function initMerchantProductsPage() {
        bindSubmit("merchantProductUpdateForm", submitMerchantProductUpdate);
        loadMerchantProducts();
    }

    function loadHomeHighlights() {
        request("/api/posts").then(function (list) {
            var hotPosts = rankItems(list, function (post) {
                return Number(post.likeCount || 0) + Number(post.commentCount || 0);
            }, 1);
            renderCards("homeHotPost", hotPosts, homePostCard);
        });

        request("/api/stores").then(function (list) {
            renderCards("homeHotStores", enabledItems(list).slice(0, 3), homeStoreCard);
        });

        request("/api/products").then(function (list) {
            var hotProducts = rankItems(enabledItems(list), function (product) {
                return Number(product.sales || 0);
            }, 4);
            renderCards("homeHotProducts", hotProducts, homeProductCard);
            notice("首页数据已刷新。");
        });
    }

    function homePostCard(post) {
        return card(post.cover || "assets/images/content/default-post.jpg", post.title, [
            "话题：" + (post.topic || "-"),
            "点赞 " + number(post.likeCount) + " / 评论 " + number(post.commentCount),
            post.content || "-"
        ], [
            ["前往社区", function () { window.location.href = "community.html"; }]
        ]);
    }

    function homeStoreCard(store) {
        return card(store.cover || "assets/images/content/store-community-center.jpg", store.name, [
            store.address || "-",
            "电话：" + (store.phone || "-"),
            "营业时间：" + (store.businessHours || "-")
        ], [
            ["前往预约", function () { window.location.href = "services.html"; }]
        ]);
    }

    function homeProductCard(product) {
        return card(product.cover || "assets/images/content/product-cat-food.jpg", product.name, [
            "分类：" + (product.category || "-"),
            "价格：" + money(product.price),
            "销量：" + number(product.sales)
        ], [
            ["前往商城", function () { window.location.href = "shop.html"; }]
        ]);
    }

    function bindLoginRoleTabs() {
        var buttons = document.querySelectorAll("[data-login-role]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                state.loginRole = button.getAttribute("data-login-role");
                forEach(buttons, function (item) {
                    item.classList.toggle("active", item === button);
                });
                notice(state.loginRole === "ADMIN" ? "当前为管理员登录。" : "当前为普通用户登录。");
            };
        });
    }

    function initHeader(page) {
        var links = document.querySelectorAll("[data-nav-page]");
        forEach(links, function (link) {
            var linkPage = link.getAttribute("data-nav-page");
            link.classList.toggle("active", linkPage === page);
            if ((linkPage === "dashboard" || linkPage === "admin") && normalizeRole(state.authUser.role) !== "ADMIN") {
                link.hidden = true;
            }
        });

        var badge = byId("userBadge");
        if (badge) {
            var roleName = normalizeRole(state.authUser.role) === "ADMIN" ? "管理员" : "普通用户";
            badge.textContent = (state.authUser.nickname || state.authUser.username || "当前用户") + " #" + state.authUser.id + " " + roleName;
        }
    }

    function bindApiInput() {
        var input = byId("apiBase");
        if (!input) {
            return;
        }
        input.value = state.apiBase;
        input.onchange = function () {
            saveApiBase(input.value);
            notice("接口地址已保存。");
        };
    }

    function bindGlobalActions() {
        document.body.addEventListener("click", function (event) {
            var target = findActionTarget(event.target);
            if (!target) {
                return;
            }
            var action = target.getAttribute("data-action");
            if (!actions[action]) {
                return;
            }
            event.preventDefault();
            actions[action](target);
        });
    }

    function bindSubmit(id, handler) {
        var form = byId(id);
        if (!form) {
            return;
        }
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            handler(form);
        });
    }

    function login(form) {
        var payload = values(form);
        saveApiBaseFromPage();
        request("/api/users/login", {method: "POST", body: payload}).then(function (user) {
            var role = normalizeRole(user.role);
            if (state.loginRole === "ADMIN" && role !== "ADMIN") {
                notice("当前账号不是管理员，不能从管理员入口登录。", true);
                return;
            }
            if (state.loginRole === "USER" && role === "ADMIN") {
                notice("管理员账号请使用管理员登录入口。", true);
                return;
            }
            saveAuthUser(user);
            window.location.href = role === "ADMIN" ? "admin.html" : "home.html";
        });
    }

    function register(form) {
        var payload = values(form);
        saveApiBaseFromPage();
        request("/api/users/register", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("注册成功，请返回登录。");
        });
    }

    function requireLogin() {
        if (state.authUser && state.authUser.id) {
            return true;
        }
        window.location.href = "index.html";
        return false;
    }

    function requireAdmin() {
        if (normalizeRole(state.authUser.role) === "ADMIN") {
            return true;
        }
        notice("当前账号不是管理员，无法进入后台管理。", true);
        setTimeout(function () {
            window.location.href = "home.html";
        }, 900);
        return false;
    }

    function requireMerchant() {
        if (isApprovedMerchant()) {
            return true;
        }
        notice("当前账号不是已审核商户，无法进入商户商品管理。", true);
        setTimeout(function () {
            window.location.href = "profile.html";
        }, 900);
        return false;
    }

    function requireMerchantServiceManager() {
        if (canManageMerchantServices()) {
            return true;
        }
        notice("当前账号不是已审核商户，无法处理门店预约。", true);
        setTimeout(function () {
            window.location.href = "services.html";
        }, 900);
        return false;
    }

    function requireMerchantStoreRegistration() {
        if (isApprovedMerchant()) {
            return true;
        }
        notice("只有审核通过的商户可以访问门店入驻页面。", true);
        setTimeout(function () {
            window.location.href = "profile.html";
        }, 900);
        return false;
    }

    function isApprovedMerchant() {
        return normalizeRole(state.authUser && state.authUser.role) !== "ADMIN"
            && Number(state.authUser && state.authUser.isMerchant) === 1;
    }

    function canManageMerchantServices() {
        return normalizeRole(state.authUser && state.authUser.role) === "ADMIN" || isApprovedMerchant();
    }

    function applyMerchantVisibility() {
        var visible = canManageMerchantServices();
        forEach(document.querySelectorAll("[data-merchant-only]"), function (node) {
            node.hidden = !visible;
        });
    }

    function logout() {
        clearAuthUser();
        window.location.href = "index.html";
    }

    function request(path, options) {
        options = options || {};
        saveApiBaseFromPage();

        var headers = options.headers || {};
        if (state.authUser && state.authUser.id) {
            headers["X-User-Id"] = String(state.authUser.id);
        }
        if (options.body && !(options.body instanceof FormData)) {
            headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(options.body);
        }

        return fetch(state.apiBase + path, {
            method: options.method || "GET",
            headers: headers,
            body: options.body
        }).then(function (response) {
            return response.json();
        }).then(function (result) {
            if (result.code !== 200) {
                throw new Error(result.message || "请求失败");
            }
            return result.data;
        }).catch(function (error) {
            notice(error.message || "请求失败", true);
            throw error;
        });
    }

    function loadDashboard() {
        request("/api/statistics/dashboard").then(function (data) {
            var overview = data && data.overview ? data.overview : {};
            renderMetrics("metrics", [
                ["用户数", overview.userCount],
                ["宠物档案", overview.petCount],
                ["发帖量", overview.postCount],
                ["预约数", overview.appointmentCount],
                ["订单数", overview.orderCount],
                ["订单总额", money(overview.totalOrderAmount)]
            ]);
            var enhanced = data && data.enhanced ? data.enhanced : {};
            var conversion = enhanced.appointmentConversion || {};
            renderMetrics("todayMetrics", [
                ["今日活跃用户", enhanced.todayActiveUserCount],
                ["今日发帖", enhanced.todayPostCount],
                ["今日预约", enhanced.todayAppointmentCount],
                ["今日订单", enhanced.todayOrderCount],
                ["今日订单金额", money(enhanced.todayOrderAmount)]
            ]);
            renderMetrics("appointmentConversion", [
                ["预约总数", conversion.totalAppointmentCount],
                ["已完成预约", conversion.finishedAppointmentCount],
                ["预约完成率", number(conversion.finishRate) + "%"],
                ["平台用户数", conversion.userCount],
                ["用户预约转化率", number(conversion.userAppointmentRate) + "%"]
            ]);
            renderRows("productTop10Rows", data && data.productTop10 ? data.productTop10 : [], function (item, index) {
                return "<tr><td>" + (index + 1) + "</td><td>" + text(item.productName) + "</td><td>" + money(item.price) +
                    "</td><td>" + number(item.sales) + "</td><td>" + number(item.stock) + "</td></tr>";
            });
            renderRows("hotPostRows", enhanced.hotPosts || [], function (item, index) {
                return "<tr><td>" + (index + 1) + "</td><td>" + text(item.title) + "</td><td>" + text(item.topic || "未选择") +
                    "</td><td>" + number(item.likeCount) + "</td><td>" + number(item.commentCount) + "</td><td>" +
                    number(item.collectCount) + "</td><td>" + number(item.score) + "</td></tr>";
            });
            renderStatusSummary("appointmentStatusList", data && data.appointmentStatus ? data.appointmentStatus : []);
            renderStatusSummary("orderStatusList", data && data.orderStatus ? data.orderStatus : []);
            var trend = enhanced.dailyTrend || [];
            renderRows("dailyTrendRows", trend, function (item) {
                return "<tr><td>" + text(item.date) + "</td><td>" + text(item.userCount) + "</td><td>" + text(item.postCount) + "</td><td>" + text(item.appointmentCount) + "</td><td>" + text(item.orderCount) + "</td><td>" + money(item.orderAmount) + "</td></tr>";
            });
            notice("统计数据已刷新。");
        });
    }

    function renderStatusSummary(containerId, items) {
        renderList(containerId, items, function (item) {
            return "<div class=\"list-item status-summary-item\"><span>" + text(item.statusName) + "</span><strong>" +
                number(item.count) + "</strong></div>";
        });
    }

    function loadNotifications() {
        request("/api/notifications?userId=" + userId()).then(function (list) {
            renderList("notificationList", list, function (item) {
                return "<div class=\"list-item\"><strong>" + text(item.content) + "</strong><p>类型：" + text(item.type) + "，目标：" + text(item.targetType) + "#" + text(item.targetId) + "</p><p>" + text(item.createTime) + "</p><div class=\"card-actions\"><button data-read-id=\"" + item.id + "\">标记已读</button></div></div>";
            });
            bindReadButtons();
        });
    }

    function bindReadButtons() {
        var buttons = document.querySelectorAll("[data-read-id]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                request("/api/notifications/" + button.getAttribute("data-read-id") + "/read?userId=" + userId(), {method: "PUT"}).then(loadNotifications);
            };
        });
    }

    function markAllNotifications() {
        request("/api/notifications/read-all?userId=" + userId(), {method: "PUT"}).then(function () {
            notice("通知已全部标记为已读。");
            loadNotifications();
        });
    }

    function createPost(form) {
        var payload = values(form);
        payload.userId = userId();
        request("/api/posts", {method: "POST", body: payload}).then(function () {
            form.reset();
            forEach(document.querySelectorAll("[data-action=\"selectPostTopic\"]"), function (button) {
                button.classList.remove("active");
            });
            notice("帖子已提交审核，审核通过后会在社区展示。");
            loadPosts();
        });
    }

    function loadPosts() {
        request("/api/posts").then(function (list) {
            renderCards("postList", list, postCard);
        });
    }

    function postCard(post) {
        var labels = [];
        if (Number(post.isPinned) === 1) {
            labels.push("【置顶】");
        }
        if (Number(post.isFeatured) === 1) {
            labels.push("【精选】");
        }
        return card(post.cover, labels.join("") + post.title, [
            "话题：" + text(post.topic),
            text(post.content),
            "点赞 " + number(post.likeCount) + " / 评论 " + number(post.commentCount) + " / 收藏 " + number(post.collectCount)
        ], [
            ["点赞", function () { postAction("/api/post-interactions/likes", post.id); }],
            ["收藏", function () { postAction("/api/post-interactions/collects", post.id); }],
            ["评论", function () { commentPost(post.id); }],
            ["举报", function () { report("POST", post.id); }]
        ], "post-detail.html?id=" + post.id);
    }

    function postAction(path, postId, onComplete) {
        request(path, {method: "POST", body: {postId: postId, userId: userId()}}).then(function () {
            notice("操作成功。");
            if (onComplete) {
                onComplete();
            } else {
                loadPosts();
            }
        });
    }

    function commentPost(postId) {
        var content = prompt("请输入评论内容");
        if (!content) {
            return;
        }
        request("/api/post-interactions/comments", {method: "POST", body: {postId: postId, userId: userId(), content: content}}).then(function () {
            notice("评论已提交。");
            loadPosts();
        });
    }

    function createTopic(form) {
        var payload = values(form);
        request("/api/topics", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("话题已提交审核，管理员通过后会展示。");
            loadTopics();
        });
    }

    function loadTopics() {
        request("/api/topics").then(function (list) {
            var node = byId("topicChips");
            if (!node) {
                return;
            }
            node.innerHTML = list && list.length ? list.map(function (item) {
                return "<span class=\"chip\">#" + text(item.name) + " " + number(item.postCount) + "</span>";
            }).join("") : emptyHtml();
        });
    }

    function loadSelectableTopics() {
        request("/api/topics").then(function (list) {
            var node = byId("topicChoices");
            if (!node) {
                return;
            }
            node.innerHTML = list && list.length ? list.map(function (item) {
                return "<button type=\"button\" class=\"chip topic-choice\" data-action=\"selectPostTopic\" data-topic-name=\"" + text(item.name) + "\">#" + text(item.name) + " " + number(item.postCount) + "</button>";
            }).join("") : emptyHtml();
        });
    }

    function selectPostTopic(button) {
        var input = byId("selectedPostTopic");
        if (!input) {
            return;
        }
        input.value = button.getAttribute("data-topic-name") || "";
        forEach(document.querySelectorAll("[data-action=\"selectPostTopic\"]"), function (item) {
            item.classList.toggle("active", item === button);
        });
        notice("已选择话题：#" + input.value);
    }

    function loadVideos() {
        request("/api/videos?limit=8").then(function (list) {
            var node = byId("videoList");
            if (!node) {
                return;
            }
            list = list || [];
            node.innerHTML = list.length ? list.map(videoCardHtml).join("") : emptyHtml();
            notice(list.length ? "已加载 " + list.length + " 条短视频。" : "暂无已通过审核的短视频。");
        });
    }

    function videoCardHtml(video) {
        return '<article class="video-card"><a href="video-detail.html?id=' + video.id + '">' +
            '<img src="' + text(img(video.cover)) + '" alt="' + text(video.title) + '">' +
            '<h2>' + text(video.title) + '</h2></a></article>';
    }

    function loadSelectableVideoTopics() {
        request("/api/topics").then(function (list) {
            var node = byId("videoTopicChoices");
            if (!node) {
                return;
            }
            node.innerHTML = list && list.length ? list.map(function (item) {
                return '<button type="button" class="chip topic-choice" data-action="selectVideoTopic" data-topic-name="' + text(item.name) + '">#' + text(item.name) + '</button>';
            }).join("") : emptyHtml();
        });
    }

    function selectVideoTopic(button) {
        var input = byId("selectedVideoTopic");
        if (!input) {
            return;
        }
        input.value = button.getAttribute("data-topic-name") || "";
        forEach(document.querySelectorAll('[data-action="selectVideoTopic"]'), function (item) {
            item.classList.toggle("active", item === button);
        });
        notice("已选择话题：#" + input.value);
    }

    function createShortVideo(form) {
        if (!form.elements.topic.value) {
            notice("请选择一个已审核话题。", true);
            return;
        }
        uploadFile("videoCoverFile", "short-video-cover").then(function (coverFile) {
            form.elements.cover.value = coverFile.url;
            return uploadFile("shortVideoFile", "short-video");
        }).then(function (videoFile) {
            form.elements.videoUrl.value = videoFile.url;
            var payload = values(form);
            payload.userId = userId();
            return request("/api/videos", {method: "POST", body: payload});
        }).then(function () {
            form.reset();
            forEach(document.querySelectorAll('[data-action="selectVideoTopic"]'), function (button) {
                button.classList.remove("active");
            });
            notice("短视频已提交管理员审核。");
        });
    }

    function searchPosts() {
        var keyword = byId("searchKeyword") ? byId("searchKeyword").value : "";
        if (!keyword) {
            notice("请输入搜索关键词。", true);
            return;
        }
        request("/api/search/posts?keyword=" + encodeURIComponent(keyword) + "&limit=50").then(function (list) {
            list = list || [];
            renderCards("postList", list, postCard);
            notice(list.length ? "已找到 " + list.length + " 条相关帖子。" : "没有找到相关帖子。");
        });
    }

    function loadPostDetail() {
        var postId = queryParam("id");
        if (!postId) {
            notice("缺少帖子ID，无法查看详情。", true);
            return;
        }
        Promise.all([
            request("/api/posts/" + postId),
            request("/api/post-interactions/comments/tree?postId=" + postId)
        ]).then(function (results) {
            renderPostDetail(results[0]);
            renderPostCommentTree(results[1] || []);
            notice("帖子详情已加载。");
        });
    }

    function renderPostDetail(post) {
        var node = byId("postDetail");
        if (!node) {
            return;
        }
        var labels = [];
        if (Number(post.isPinned) === 1) {
            labels.push("【置顶】");
        }
        if (Number(post.isFeatured) === 1) {
            labels.push("【精选】");
        }
        node.innerHTML = "<article class=\"post-detail-content\">" +
            "<img class=\"post-detail-cover\" src=\"" + img(post.cover) + "\" alt=\"帖子图片\">" +
            "<div><h2>" + text(labels.join("") + post.title) + "</h2>" +
            "<p class=\"post-meta\">话题：" + text(post.topic) + "</p>" +
            "<p>" + text(post.content) + "</p>" +
            "<p class=\"post-meta\">点赞 " + number(post.likeCount) + " / 评论 " + number(post.commentCount) + " / 收藏 " + number(post.collectCount) + "</p></div>" +
            "</article>";
    }

    function renderPostCommentTree(comments) {
        var node = byId("postCommentTree");
        if (!node) {
            return;
        }
        node.innerHTML = comments.length ? "<div class=\"comment-list\">" + comments.map(commentTreeHtml).join("") + "</div>" : emptyHtml();
    }

    function loadVideoDetail() {
        var videoId = queryParam("id");
        if (!videoId) {
            notice("缺少短视频ID，无法查看详情。", true);
            return;
        }
        Promise.all([
            request("/api/videos/" + videoId),
            request("/api/post-interactions/comments/tree?postId=" + videoId)
        ]).then(function (results) {
            renderVideoDetail(results[0]);
            renderVideoCommentTree(results[1] || []);
            notice("短视频详情已加载。");
        });
    }

    function renderVideoDetail(video) {
        var node = byId("videoDetail");
        if (!node) {
            return;
        }
        node.innerHTML = '<article class="video-detail-content">' +
            '<video controls preload="metadata" poster="' + text(img(video.cover)) + '" src="' + text(resourceUrl(video.videoUrl)) + '"></video>' +
            '<div class="video-detail-copy"><h2>' + text(video.title) + '</h2>' +
            '<p class="post-meta">话题：' + text(video.topic) + '</p>' +
            '<p>' + text(video.content) + '</p>' +
            '<p class="post-meta">点赞 ' + number(video.likeCount) + ' / 评论 ' + number(video.commentCount) + ' / 收藏 ' + number(video.collectCount) + '</p>' +
            '<div class="card-actions video-actions"><button data-video-action="like">点赞</button>' +
            '<button data-video-action="collect">收藏</button>' +
            '<button class="danger" data-video-action="report">举报</button></div></div></article>';
        bindVideoDetailButtons(video.id);
    }

    function renderVideoCommentTree(comments) {
        var node = byId("videoCommentTree");
        if (!node) {
            return;
        }
        node.innerHTML = comments.length ? '<div class="comment-list">' + comments.map(commentTreeHtml).join("") + '</div>' : emptyHtml();
    }

    function bindVideoDetailButtons(videoId) {
        forEach(document.querySelectorAll("[data-video-action]"), function (button) {
            button.onclick = function () {
                var action = button.getAttribute("data-video-action");
                if (action === "like") {
                    postAction("/api/post-interactions/likes", videoId, loadVideoDetail);
                } else if (action === "collect") {
                    postAction("/api/post-interactions/collects", videoId, loadVideoDetail);
                } else if (action === "report") {
                    report("POST", videoId);
                }
            };
        });
    }

    function submitVideoComment(form) {
        var videoId = queryParam("id");
        if (!videoId) {
            notice("缺少短视频ID，无法提交评论。", true);
            return;
        }
        var content = values(form).content;
        request("/api/post-interactions/comments", {
            method: "POST",
            body: {postId: videoId, userId: userId(), content: content}
        }).then(function () {
            form.reset();
            notice("评论已提交。");
            loadVideoDetail();
        });
    }

    function commentTreeHtml(comment) {
        var children = comment.children || [];
        return "<article class=\"comment-item\"><strong>用户 #" + text(comment.userId) + "</strong>" +
            "<p>" + text(comment.content) + "</p>" +
            "<small>" + text(comment.createTime) + "</small>" +
            (children.length ? "<div class=\"comment-children\">" + children.map(commentTreeHtml).join("") + "</div>" : "") +
            "</article>";
    }

    function createPet(form) {
        var payload = values(form);
        payload.userId = userId();
        payload.age = intVal(payload.age);
        payload.weight = decimalVal(payload.weight);
        payload.vaccinated = intVal(payload.vaccinated);
        payload.neutered = intVal(payload.neutered);
        request("/api/pets", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("宠物档案已保存。");
            loadPets();
        });
    }

    function loadPets() {
        request("/api/pets?userId=" + userId()).then(function (list) {
            renderCards("petList", list, function (pet) {
                return card(pet.avatar, pet.name, [
                    text(pet.species) + " / " + text(pet.breed),
                    "年龄：" + text(pet.age) + "，体重：" + text(pet.weight) + "kg",
                    "疫苗：" + yesNo(pet.vaccinated) + "，绝育：" + yesNo(pet.neutered),
                    text(pet.remark)
                ], [
                    ["删除", function () { deletePet(pet.id); }, "danger"]
                ]);
            });
        });
    }

    function deletePet(id) {
        request("/api/pets/" + id + "?userId=" + userId(), {method: "DELETE"}).then(function () {
            notice("档案已删除。");
            loadPets();
        });
    }

    function createStore(form) {
        var payload = values(form);
        payload.longitude = decimalVal(payload.longitude);
        payload.latitude = decimalVal(payload.latitude);
        if (payload.longitude === null || payload.latitude === null) {
            notice("请先在地图中定位门店位置。", true);
            return;
        }
        request("/api/stores", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("门店已创建。");
            var coordinateText = byId("storeCoordinateText");
            if (coordinateText) {
                coordinateText.textContent = "尚未选择门店位置。";
            }
        });
    }

    function initNearbyStoreMap() {
        if (!byId("nearbyStoreMap")) {
            return;
        }
        state.nearbyStoreMap = createBaiduMap("nearbyStoreMap");
        if (!state.nearbyStoreMap) {
            return;
        }
        state.nearbyStoreMap.addEventListener("click", function (event) {
            var point = event.latlng || event.point;
            if (point) {
                setNearbyStoreLocation(point, true);
            }
        });
        locateNearbyStores();
    }

    function locateNearbyStores() {
        locateCurrentPoint(function (point) {
            setNearbyStoreLocation(point, true);
            notice("已定位当前位置，并按距离加载附近门店。");
        }, function (message) {
            setLocationStatus("nearbyLocationStatus", message);
            notice(message, true);
        });
    }

    function setNearbyStoreLocation(point, loadNearby) {
        byId("longitude").value = Number(point.lng).toFixed(6);
        byId("latitude").value = Number(point.lat).toFixed(6);
        state.nearbyStorePoint = point;
        renderNearbyStoreMarkers([]);
        setLocationStatus("nearbyLocationStatus", "当前位置：" + Number(point.lng).toFixed(6) + "，" + Number(point.lat).toFixed(6));
        if (loadNearby) {
            loadNearbyStores();
        }
    }

    function initStoreLocationMap() {
        if (!byId("storeLocationMap")) {
            return;
        }
        state.storeLocationMap = createBaiduMap("storeLocationMap");
        if (!state.storeLocationMap) {
            return;
        }
        state.storeLocationMap.addEventListener("click", function (event) {
            var point = event.latlng || event.point;
            if (point) {
                setStoreLocation(point, true);
            }
        });
        locateStoreLocation();
    }

    function locateStoreLocation() {
        locateCurrentPoint(function (point) {
            setStoreLocation(point, false);
            notice("已定位当前位置，请确认门店位置后填写资料。");
        }, function (message) {
            setLocationStatus("storeLocationStatus", message);
            notice(message, true);
        });
    }

    function setStoreLocation(point, fillAddress) {
        var form = byId("storeForm");
        if (!form) {
            return;
        }
        form.elements.longitude.value = Number(point.lng).toFixed(6);
        form.elements.latitude.value = Number(point.lat).toFixed(6);
        showMapPoint(state.storeLocationMap, point);
        setLocationStatus("storeLocationStatus", "已选择门店位置，可以点击地图继续调整。");
        var coordinateText = byId("storeCoordinateText");
        if (coordinateText) {
            coordinateText.textContent = "经度 " + Number(point.lng).toFixed(6) + "，纬度 " + Number(point.lat).toFixed(6);
        }
        if (fillAddress) {
            fillStoreAddress(point, form);
        }
    }

    function createBaiduMap(containerId) {
        if (!window.BMapGL || typeof window.BMapGL.Map !== "function") {
            notice("百度地图加载失败，请检查 AK 和 Referer 白名单。", true);
            return null;
        }
        var map = new BMapGL.Map(containerId);
        map.centerAndZoom(new BMapGL.Point(116.404, 39.915), 13);
        map.enableScrollWheelZoom(true);
        map.addControl(new BMapGL.ZoomControl());
        return map;
    }

    function locateCurrentPoint(success, failure) {
        if (!window.BMapGL || typeof window.BMapGL.Geolocation !== "function") {
            failure("百度地图定位组件加载失败。");
            return;
        }
        var geolocation = new BMapGL.Geolocation();
        geolocation.getCurrentPosition(function (result) {
            var status = typeof this.getStatus === "function" ? this.getStatus() : null;
            if (result && result.point && (status === 0 || status === window.BMAP_STATUS_SUCCESS)) {
                success(result.point);
                return;
            }
            failure("无法获取当前位置，请允许浏览器定位，或点击地图手动选择位置。");
        }, {enableHighAccuracy: true});
    }

    function showMapPoint(map, point) {
        if (!map) {
            return;
        }
        map.clearOverlays();
        map.addOverlay(new BMapGL.Marker(point));
        map.centerAndZoom(point, 16);
    }

    function renderNearbyStoreMarkers(list) {
        var map = state.nearbyStoreMap;
        if (!map) {
            return;
        }
        map.clearOverlays();
        if (state.nearbyStorePoint) {
            addNearbyCurrentLocationMarker(map, state.nearbyStorePoint);
            map.centerAndZoom(state.nearbyStorePoint, 15);
        }
        (list || []).forEach(function (item) {
            var store = item.store || {};
            var longitude = Number(store.longitude);
            var latitude = Number(store.latitude);
            if (!isFinite(longitude) || !isFinite(latitude)) {
                return;
            }
            var point = new BMapGL.Point(longitude, latitude);
            var marker = new BMapGL.Marker(point);
            var label = new BMapGL.Label(text(store.name), {
                position: point,
                offset: new BMapGL.Size(16, -20)
            });
            label.setStyle({
                color: "#176b4d",
                backgroundColor: "#ffffff",
                border: "1px solid #9dd8bf",
                borderRadius: "4px",
                padding: "3px 6px",
                fontSize: "12px"
            });
            var content = [
                "<div style='line-height:1.7'>",
                "<div>" + text(store.address) + "</div>",
                "<div>\u8425\u4e1a\u65f6\u95f4\uff1a" + text(store.businessHours) + "</div>",
                "<div>\u8ddd\u79bb\uff1a" + text(item.distanceKm) + " km</div>",
                "<div>\u7535\u8bdd\uff1a" + text(store.phone) + "</div>",
                "</div>"
            ].join("");
            var infoWindow = new BMapGL.InfoWindow(content, {
                title: text(store.name),
                width: 260
            });
            var selectStore = function () {
                map.openInfoWindow(infoWindow, point);
                focusNearbyStoreCard(store.id);
            };
            marker.addEventListener("click", selectStore);
            if (typeof label.addEventListener === "function") {
                label.addEventListener("click", selectStore);
            }
            map.addOverlay(marker);
            map.addOverlay(label);
        });
    }

    function addNearbyCurrentLocationMarker(map, point) {
        var marker = new BMapGL.Marker(point);
        var label = new BMapGL.Label("\u5f53\u524d\u4f4d\u7f6e", {
            position: point,
            offset: new BMapGL.Size(16, -20)
        });
        label.setStyle({
            color: "#0066cc",
            backgroundColor: "#ffffff",
            border: "1px solid #7cb7f2",
            borderRadius: "4px",
            padding: "3px 6px",
            fontSize: "12px",
            fontWeight: "bold"
        });
        map.addOverlay(marker);
        map.addOverlay(label);
    }

    function bindNearbyStoreCards(list) {
        var container = byId("storeList");
        if (!container) {
            return;
        }
        var cards = container.querySelectorAll(".card");
        forEach(cards, function (cardNode, index) {
            var item = (list || [])[index] || {};
            var store = item.store || {};
            if (store.id !== null && store.id !== undefined) {
                cardNode.setAttribute("data-store-id", store.id);
            }
        });
    }

    function focusNearbyStoreCard(storeId) {
        var cardNode = document.querySelector('[data-store-id="' + text(storeId) + '"]');
        if (!cardNode) {
            return;
        }
        cardNode.scrollIntoView({behavior: "smooth", block: "center"});
        if (state.storeCardHighlightTimer) {
            window.clearTimeout(state.storeCardHighlightTimer);
        }
        cardNode.classList.remove("store-card-highlight");
        void cardNode.offsetWidth;
        cardNode.classList.add("store-card-highlight");
        state.storeCardHighlightTimer = window.setTimeout(function () {
            cardNode.classList.remove("store-card-highlight");
            state.storeCardHighlightTimer = null;
        }, 5000);
    }

    function fillStoreAddress(point, form) {
        if (!window.BMapGL || typeof window.BMapGL.Geocoder !== "function") {
            return;
        }
        var geocoder = new BMapGL.Geocoder();
        geocoder.getLocation(point, function (result) {
            if (result && result.address) {
                form.elements.address.value = result.address;
            }
        });
    }

    function setLocationStatus(id, message) {
        var node = byId(id);
        if (node) {
            node.textContent = message;
        }
    }

    function loadStores() {
        request("/api/stores").then(function (list) {
            state.stores = list || [];
            renderStores(state.stores);
            loadServiceItemsFromStores();
            var scheduleContent = byId("scheduleCalendarContent");
            if (scheduleContent && !scheduleContent.hidden && !state.schedules.length && state.stores.length) {
                loadSchedules(state.stores[0].id);
            }
        });
    }

    function loadNearbyStores() {
        var longitudeInput = byId("longitude");
        var latitudeInput = byId("latitude");
        var lng = longitudeInput && longitudeInput.value;
        var lat = latitudeInput && latitudeInput.value;
        if (!lng || !lat) {
            notice("请先通过地图完成当前位置定位。", true);
            return;
        }
        request("/api/stores/nearby?longitude=" + encodeURIComponent(lng) + "&latitude=" + encodeURIComponent(lat) + "&limit=20").then(function (list) {
            renderNearbyStoreMarkers(list || []);
            renderCards("storeList", list, function (item) {
                var store = item.store || {};
                return card(store.cover, store.name, [
                    text(store.address),
                    "距离：" + text(item.distanceKm) + " km",
                    "电话：" + text(store.phone)
                ], [
                    ["查看服务", function () { loadServiceItems(store.id); }],
                    ["查看排班", function () { loadSchedules(store.id); }]
                ]);
            });
            bindNearbyStoreCards(list || []);
        });
    }

    function renderStores(list) {
        renderCards("storeList", list, function (store) {
            return card(store.cover, store.name, [
                text(store.address),
                "电话：" + text(store.phone),
                "营业：" + text(store.businessHours)
            ], [
                ["服务项目", function () { loadServiceItems(store.id); }],
                ["排班", function () { loadSchedules(store.id); }]
            ]);
        });
    }

    function createServiceItem(form) {
        var payload = values(form);
        payload.storeId = intVal(payload.storeId);
        payload.price = decimalVal(payload.price);
        payload.duration = intVal(payload.duration);
        request("/api/service-items", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("服务项目已新增。");
            loadServiceItems(payload.storeId);
        });
    }

    function loadServiceItems(storeId) {
        if (!storeId) {
            renderServiceItems([]);
            return;
        }
        request("/api/service-items?storeId=" + storeId).then(function (list) {
            state.serviceItems = list || [];
            renderServiceItems(state.serviceItems);
            notice("已加载门店 " + storeId + " 的服务项目。");
        });
    }

    function loadServiceItemsFromStores() {
        state.serviceItems = [];
        renderServiceItems([]);
        forEach(state.stores.slice(0, 5), function (store) {
            request("/api/service-items?storeId=" + store.id).then(function (list) {
                state.serviceItems = state.serviceItems.concat(list || []);
                renderServiceItems(state.serviceItems);
            });
        });
    }

    function renderServiceItems(list) {
        renderCards("serviceItemList", list || [], function (item) {
            return card(item.cover, item.name, [
                "门店ID：" + text(item.storeId),
                "分类：" + text(item.category),
                "价格：" + money(item.price),
                "时长：" + text(item.duration) + " 分钟"
            ], [
                ["带入预约", function () { fillServiceForAppointment(item); }]
            ]);
        });
    }

    function fillServiceForAppointment(item) {
        var form = byId("appointmentForm");
        if (!form) {
            return;
        }
        form.elements.storeId.value = item.storeId || "";
        form.elements.serviceItemId.value = item.id || "";
        notice("已带入服务项目 " + item.id + "。");
    }

    function createSchedule(form) {
        var payload = values(form);
        payload.storeId = intVal(payload.storeId);
        payload.serviceItemId = intVal(payload.serviceItemId);
        payload.capacity = intVal(payload.capacity);
        request("/api/service-schedules", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("排班已创建。");
            loadSchedules(payload.storeId);
        });
    }

    function toggleScheduleCalendar(button) {
        var content = byId("scheduleCalendarContent");
        if (!content) {
            return;
        }
        var expanded = content.hidden;
        setScheduleCalendarExpanded(expanded);
        if (expanded && !state.schedules.length) {
            if (state.stores.length) {
                loadSchedules(state.stores[0].id);
            } else {
                var feedback = byId("scheduleFeedback");
                if (feedback) {
                    feedback.textContent = "门店正在加载，请稍候。";
                }
            }
        }
    }

    function setScheduleCalendarExpanded(expanded) {
        var content = byId("scheduleCalendarContent");
        var button = document.querySelector("[data-action=\"toggleScheduleCalendar\"]");
        if (content) {
            content.hidden = !expanded;
        }
        if (button) {
            button.setAttribute("aria-expanded", expanded ? "true" : "false");
            button.textContent = expanded ? "收起排班" : "展开排班";
        }
    }

    function loadSchedules(storeId) {
        if (!storeId) {
            return;
        }
        setScheduleCalendarExpanded(true);
        state.scheduleStoreId = storeId;
        Promise.all([
            request("/api/service-schedules?storeId=" + storeId),
            request("/api/service-items?storeId=" + storeId)
        ]).then(function (results) {
            state.schedules = results[0] || [];
            mergeServiceItems(storeId, results[1] || []);
            renderSchedules();
            var scheduleFeedback = byId("scheduleFeedback");
            var schedulePanel = byId("schedulePanel");
            var storeName = scheduleStoreName({storeId: storeId});
            if (scheduleFeedback) {
                scheduleFeedback.textContent = state.schedules.length
                    ? "已加载" + storeName + "的 " + state.schedules.length + " 条排班。"
                    : storeName + "暂无排班。";
            }
            notice(scheduleFeedback ? scheduleFeedback.textContent : "排班已加载。");
            if (schedulePanel) {
                schedulePanel.scrollIntoView({behavior: "smooth", block: "start"});
            }
        });
    }

    function mergeServiceItems(storeId, items) {
        state.serviceItems = state.serviceItems.filter(function (item) {
            return String(item.storeId) !== String(storeId);
        }).concat(items || []);
    }

    function renderSchedules() {
        renderRows("scheduleRows", state.schedules, function (item) {
            return "<tr><td>" + item.id + "</td><td>" + text(scheduleStoreName(item)) + "</td><td>" + text(scheduleServiceName(item)) + "</td><td>" + text(item.scheduleDate) + "</td><td>" + text(item.startTime) + " - " + text(item.endTime) + "</td><td>" + number(item.bookedCount) + "/" + number(item.capacity) + "</td><td><button data-appointment-schedule=\"" + item.id + "\">带入预约</button></td></tr>";
        });
        bindScheduleButtons();
    }

    function scheduleStoreName(item) {
        var store = findById(state.stores, item.storeId);
        return store.name ? store.name + "（ID：" + item.storeId + "）" : "门店 ID：" + item.storeId;
    }

    function scheduleServiceName(item) {
        var serviceItem = findById(state.serviceItems, item.serviceItemId);
        return serviceItem.name ? serviceItem.name + "（ID：" + item.serviceItemId + "）" : "服务 ID：" + item.serviceItemId;
    }

    function bindScheduleButtons() {
        var buttons = document.querySelectorAll("[data-appointment-schedule]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                var id = button.getAttribute("data-appointment-schedule");
                var schedule = findById(state.schedules, id);
                var form = byId("appointmentForm");
                if (!form) {
                    return;
                }
                form.elements.serviceScheduleId.value = id;
                form.elements.storeId.value = schedule.storeId || "";
                form.elements.serviceItemId.value = schedule.serviceItemId || "";
                form.elements.appointmentTime.value = toDateTimeLocal(schedule.scheduleDate, schedule.startTime);
                form.scrollIntoView({behavior: "smooth", block: "start"});
                notice("已将排班 " + id + " 带入预约，请选择宠物并确认提交。");
            };
        });
    }

    function createAppointment(form) {
        var payload = values(form);
        payload.userId = userId();
        payload.petId = intVal(payload.petId);
        payload.storeId = intVal(payload.storeId);
        payload.serviceItemId = intVal(payload.serviceItemId);
        payload.serviceScheduleId = intVal(payload.serviceScheduleId);
        payload.appointmentTime = normalizeDateTime(payload.appointmentTime);
        if (!payload.serviceScheduleId) {
            notice("请先从排班日历选择可预约时段。", true);
            return;
        }
        request("/api/appointments", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("预约申请已提交，请等待商户确认。");
            loadSchedules(payload.storeId);
        });
    }

    function loadMyAppointments() {
        Promise.all([
            request("/api/appointments/user?userId=" + userId()),
            request("/api/stores"),
            request("/api/pets?userId=" + userId())
        ]).then(function (results) {
            var appointments = results[0] || [];
            var stores = results[1] || [];
            var pets = results[2] || [];
            return loadAppointmentServices(appointments).then(function (services) {
                renderList("myAppointmentList", appointments, function (item) {
                    var store = findById(stores, item.storeId);
                    var serviceItem = findById(services, item.serviceItemId);
                    var pet = findById(pets, item.petId);
                    var action = Number(item.status) === 1 || Number(item.status) === 3
                        ? '<div class="card-actions"><button class="danger" data-my-appointment-cancel="' + item.id + '">取消预约</button></div>'
                        : "";
                    return '<article class="list-item appointment-item">' +
                        '<strong>预约 #' + text(item.id) + '</strong>' +
                        '<p>门店：' + text(store.name || ("ID " + item.storeId)) +
                        '，服务：' + text(serviceItem.name || ("ID " + item.serviceItemId)) + '</p>' +
                        '<p>宠物：' + text(pet.name || ("ID " + item.petId)) +
                        '，时间：' + text(item.appointmentTime) + '</p>' +
                        '<p>状态：' + text(appointmentStatus(item.status)) +
                        '，备注：' + text(item.remark) + '</p>' + action + '</article>';
                });
                bindMyAppointmentButtons();
                notice("预约记录已加载。");
            });
        });
    }

    function loadAppointmentServices(appointments) {
        var storeIds = [];
        forEach(appointments || [], function (appointment) {
            if (storeIds.indexOf(String(appointment.storeId)) < 0) {
                storeIds.push(String(appointment.storeId));
            }
        });
        return Promise.all(storeIds.map(function (storeId) {
            return request("/api/service-items?storeId=" + encodeURIComponent(storeId));
        })).then(function (groups) {
            var services = [];
            forEach(groups, function (group) {
                services = services.concat(group || []);
            });
            return services;
        });
    }

    function bindMyAppointmentButtons() {
        forEach(document.querySelectorAll("[data-my-appointment-cancel]"), function (button) {
            button.onclick = function () {
                var id = button.getAttribute("data-my-appointment-cancel");
                if (!window.confirm("确认取消这条预约吗？")) {
                    return;
                }
                request("/api/appointments/" + id + "/cancel?userId=" + userId(), {method: "PUT"}).then(function () {
                    notice("预约已取消。");
                    loadMyAppointments();
                });
            };
        });
    }

    function loadMerchantAppointmentStores() {
        request("/api/stores").then(function (list) {
            var stores = enabledItems(list || []);
            if (normalizeRole(state.authUser && state.authUser.role) !== "ADMIN") {
                stores = stores.filter(function (store) {
                    return String(store.merchantUserId) === String(userId());
                });
            }
            state.stores = stores;
            var selector = byId("merchantAppointmentStoreId");
            if (!selector) {
                return;
            }
            selector.innerHTML = stores.map(function (store) {
                return '<option value="' + text(store.id) + '">' + text(store.name) + '（ID：' + text(store.id) + '）</option>';
            }).join("");
            if (!stores.length) {
                renderList("merchantAppointmentList", []);
                notice("当前账号暂无可管理的门店。", true);
                return;
            }
            loadMerchantAppointments();
        });
    }

    function loadMerchantAppointments() {
        var selector = byId("merchantAppointmentStoreId");
        var storeId = selector && selector.value;
        if (!storeId) {
            renderList("merchantAppointmentList", []);
            return;
        }
        Promise.all([
            request("/api/appointments/store?storeId=" + encodeURIComponent(storeId)),
            request("/api/service-items?storeId=" + encodeURIComponent(storeId))
        ]).then(function (results) {
            var appointments = results[0] || [];
            var services = results[1] || [];
            renderList("merchantAppointmentList", appointments, function (item) {
                var serviceItem = findById(services, item.serviceItemId);
                var actionsHtml = "";
                if (Number(item.status) === 3) {
                    actionsHtml = '<div class="card-actions"><button data-merchant-appointment="' + item.id +
                        '" data-appointment-status="1">通过预约</button> ' +
                        '<button class="danger" data-merchant-appointment="' + item.id +
                        '" data-appointment-status="0">驳回预约</button></div>';
                } else if (Number(item.status) === 1) {
                    actionsHtml = '<div class="card-actions"><button data-merchant-appointment="' + item.id +
                        '" data-appointment-status="2">标记完成</button> ' +
                        '<button class="danger" data-merchant-appointment="' + item.id +
                        '" data-appointment-status="0">取消预约</button></div>';
                }
                return '<article class="list-item appointment-item">' +
                    '<strong>预约 #' + text(item.id) + '</strong>' +
                    '<p>用户 ID：' + text(item.userId) + '，宠物 ID：' + text(item.petId) + '</p>' +
                    '<p>服务：' + text(serviceItem.name || ("ID " + item.serviceItemId)) +
                    '，时间：' + text(item.appointmentTime) + '</p>' +
                    '<p>状态：' + text(appointmentStatus(item.status)) +
                    '，备注：' + text(item.remark) + '</p>' + actionsHtml + '</article>';
            });
            bindMerchantAppointmentButtons();
            notice("门店预约已加载。");
        });
    }

    function bindMerchantAppointmentButtons() {
        forEach(document.querySelectorAll("[data-merchant-appointment]"), function (button) {
            button.onclick = function () {
                var id = button.getAttribute("data-merchant-appointment");
                var status = intVal(button.getAttribute("data-appointment-status"));
                updateMerchantAppointment(id, status);
            };
        });
    }

    function updateMerchantAppointment(id, status) {
        var promptText = status === 1
            ? "请输入通过备注"
            : (status === 2 ? "请输入完成备注" : "请输入取消或驳回原因");
        var remark = window.prompt(promptText, "");
        if (remark === null) {
            return;
        }
        request("/api/appointments/" + id + "/status", {
            method: "PUT",
            body: {status: status, remark: remark}
        }).then(function () {
            notice(status === 1 ? "预约已通过并占用对应排班名额。" : "预约状态已更新。");
            loadMerchantAppointments();
        });
    }

    function createProduct(form) {
        var payload = values(form);
        payload.price = decimalVal(payload.price);
        payload.stock = intVal(payload.stock);
        request("/api/products", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("商品已创建。");
            loadProducts();
        });
    }

    function submitMerchantProductCreate(form) {
        var payload = values(form);
        payload.price = decimalVal(payload.price);
        payload.stock = intVal(payload.stock);
        request("/api/merchant/product-applications", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("商品新增申请已提交，管理员通过后才会展示。", false);
        });
    }

    function submitMerchantProductUpdate(form) {
        var payload = values(form);
        var productId = intVal(payload.id);
        delete payload.id;
        payload.price = decimalVal(payload.price);
        payload.stock = intVal(payload.stock);
        request("/api/merchant/product-applications/" + productId, {method: "PUT", body: payload}).then(function () {
            form.reset();
            notice("商品修改申请已提交。", false);
            loadMerchantProducts();
        });
    }

    function loadMerchantProducts() {
        Promise.all([
            request("/api/merchant/products"),
            request("/api/merchant/product-applications")
        ]).then(function (results) {
            var products = results[0] || [];
            var applications = results[1] || [];
            renderCards("merchantProductList", products, function (product) {
                return card(product.cover, product.name, [
                    "商品ID：" + product.id,
                    "分类：" + text(product.category),
                    "价格：" + money(product.price),
                    "库存：" + number(product.stock),
                    "状态：" + (Number(product.status) === 1 ? "已上架" : "已下架")
                ], [
                    ["带入修改", function () { fillMerchantProductUpdate(product); }],
                    [Number(product.status) === 1 ? "申请下架" : "申请上架", function () {
                        submitMerchantProductStatus(product.id, Number(product.status) === 1 ? "disable" : "enable");
                    }]
                ]);
            });
            renderList("merchantProductApplicationList", applications, function (item) {
                return "<div class=\"list-item\"><strong>" + text(item.operationType) + " 申请 #" + item.id + "</strong>" +
                    "<p>商品ID：" + text(item.productId) + "，商品：" + text(item.productName) + "</p>" +
                    "<p>状态：" + productApplicationStatus(item.status) + "</p></div>";
            });
        });
    }

    function fillMerchantProductUpdate(product) {
        var form = byId("merchantProductUpdateForm");
        if (!form) {
            return;
        }
        form.elements.id.value = product.id || "";
        form.elements.name.value = product.name || "";
        form.elements.category.value = product.category || "主粮";
        form.elements.price.value = product.price || "";
        form.elements.stock.value = product.stock || "";
        form.elements.cover.value = product.cover || "";
        form.elements.description.value = product.description || "";
        form.scrollIntoView({behavior: "smooth", block: "start"});
    }

    function submitMerchantProductStatus(productId, action) {
        request("/api/merchant/product-applications/" + productId + "/" + action, {method: "POST"}).then(function () {
            notice(action === "enable" ? "上架申请已提交。" : "下架申请已提交。");
            loadMerchantProducts();
        });
    }

    function loadProducts() {
        request("/api/products").then(function (list) {
            state.products = list || [];
            renderCards("productList", state.products, function (product) {
                return card(product.cover, product.name, [
                    "分类：" + text(product.category),
                    "价格：" + money(product.price),
                    "库存：" + number(product.stock) + "，销量：" + number(product.sales)
                ], [
                    ["加入购物车", function () { addCart(product.id); }]
                ]);
            });
        });
    }

    function addCart(productId) {
        var quantity = prompt("请输入数量", "1");
        if (!quantity) {
            return;
        }
        request("/api/cart-items", {method: "POST", body: {userId: userId(), productId: productId, quantity: intVal(quantity)}}).then(function () {
            notice("已加入购物车。");
            loadCart();
        });
    }

    function loadCart() {
        request("/api/cart-items?userId=" + userId()).then(function (list) {
            renderList("cartList", list, function (item) {
                return "<div class=\"list-item\"><strong>购物车项 #" + item.id + "</strong><p>商品ID：" + item.productId + "，数量：" + item.quantity + "</p><div class=\"card-actions\"><button data-cart-delete=\"" + item.id + "\" class=\"danger\">移除</button></div></div>";
            });
            bindCartButtons();
        });
    }

    function bindCartButtons() {
        var buttons = document.querySelectorAll("[data-cart-delete]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                request("/api/cart-items/" + button.getAttribute("data-cart-delete") + "?userId=" + userId(), {method: "DELETE"}).then(loadCart);
            };
        });
    }

    function goCheckout() {
        window.location.href = "checkout.html";
    }

    function goShop() {
        window.location.href = "shop.html";
    }

    function loadCheckout() {
        Promise.all([
            request("/api/cart-items?userId=" + userId()),
            request("/api/products")
        ]).then(function (results) {
            state.checkoutCart = results[0] || [];
            state.products = results[1] || [];
            var total = 0;
            renderList("checkoutItems", state.checkoutCart, function (item) {
                var product = findById(state.products, item.productId);
                var quantity = Number(item.quantity || 0);
                var price = Number(product.price || 0);
                var subtotal = price * quantity;
                total += subtotal;
                return "<div class=\"list-item checkout-item\">" +
                    "<img src=\"" + img(product.cover) + "\" alt=\"商品图片\">" +
                    "<div><strong>" + text(product.name || ("商品 #" + item.productId)) + "</strong>" +
                    "<p>单价：¥" + money(price) + "</p>" +
                    "<p>数量：" + quantity + "</p></div>" +
                    "<strong class=\"checkout-subtotal\">¥" + money(subtotal) + "</strong>" +
                    "</div>";
            });

            var totalNode = byId("checkoutTotal");
            var checkoutSubmit = byId("checkoutSubmit");
            if (totalNode) {
                totalNode.textContent = "¥" + money(total);
            }
            if (checkoutSubmit) {
                checkoutSubmit.disabled = state.checkoutCart.length === 0;
            }
            notice(state.checkoutCart.length ? "请核对商品并确认模拟结算。" : "购物车为空，请返回商城添加商品。", state.checkoutCart.length === 0);
        });
    }

    function submitCheckout(form) {
        var payload = values(form);
        payload.userId = userId();
        request("/api/product-orders", {method: "POST", body: payload}).then(function (orderId) {
            form.reset();
            var result = byId("checkoutResult");
            if (result) {
                result.textContent = "模拟结算成功，订单ID：" + orderId + "。可返回商城查看订单。";
            }
            notice("订单已创建，ID：" + orderId);
            loadCheckout();
        });
    }

    function toggleOrders(button) {
        var cartView = byId("cartView");
        var orderView = byId("orderView");
        var title = byId("shopListTitle");
        if (!cartView || !orderView) {
            return;
        }
        var showOrders = orderView.hidden;
        cartView.hidden = showOrders;
        orderView.hidden = !showOrders;
        button.textContent = showOrders ? "取消查看" : "查看订单";
        if (title) {
            title.textContent = showOrders ? "我的订单" : "购物车";
        }
        if (showOrders) {
            loadOrders();
        } else {
            loadCart();
        }
    }

    function loadOrders() {
        request("/api/product-orders?userId=" + userId()).then(function (list) {
            renderList("orderList", list, function (item) {
                return "<div class=\"list-item\"><strong>订单 " + text(item.orderNo) + "</strong><p>金额：" + money(item.totalAmount) + "，状态：" + orderStatus(item.status) + "</p><div class=\"card-actions\"><button data-order-cancel=\"" + item.id + "\" class=\"danger\">取消</button></div></div>";
            });
            bindOrderButtons();
        });
    }

    function bindOrderButtons() {
        var buttons = document.querySelectorAll("[data-order-cancel]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                request("/api/product-orders/" + button.getAttribute("data-order-cancel") + "/cancel?userId=" + userId(), {method: "PUT"}).then(loadOrders);
            };
        });
    }

    function loadProfile() {
        var id = byId("profileUserId") && byId("profileUserId").value ? byId("profileUserId").value : userId();
        state.profileUserId = String(id);
        request("/api/users/" + id + "/profile?viewerId=" + userId()).then(function (data) {
            var user = data.user || {};
            var count = data.followCount || {};
            var ownProfile = String(id) === String(userId());
            state.profileFollowed = !!data.followedByViewer;
            if (ownProfile) {
                saveAuthUser(user);
            }
            byId("profilePanel").innerHTML =
                "<div class=\"profile-head\">" +
                "<img class=\"avatar\" src=\"" + img(user.avatar) + "\" alt=\"头像\">" +
                "<div><h2>" + text(user.nickname || user.username) + "</h2><p>用户ID：" + text(user.id) + "，角色：" + text(user.role) + "</p><p>" + (data.followedByViewer ? "当前用户已关注" : "当前用户未关注") + "</p></div>" +
                "</div>" +
                "<div class=\"profile-stats\">" +
                stat("关注", count.followingCount) +
                stat("粉丝", count.followerCount) +
                stat("帖子", data.postCount) +
                stat("宠物档案", data.petCount) +
                stat("获赞帖子", data.likedPostCount) +
                stat("收藏帖子", data.collectedPostCount) +
                "</div>" +
                "<h2>近期帖子</h2>" +
                (data.recentPosts && data.recentPosts.length ? data.recentPosts.map(function (post) {
                    return "<div class=\"list-item\"><strong>" + text(post.title) + "</strong><p>" + text(post.content) + "</p></div>";
                }).join("") : emptyHtml());
            renderProfileFollowAction(ownProfile);
            loadProfileSocialLists(id);
            var merchantPanel = byId("merchantApplicationPanel");
            if (merchantPanel) {
                merchantPanel.hidden = !ownProfile;
            }
            updateStoreRegistrationEntry(ownProfile);
            if (ownProfile) {
                loadMerchantApplication();
            }
        });
    }

    function renderProfileFollowAction(ownProfile) {
        var container = byId("profileFollowAction");
        if (!container) {
            return;
        }
        container.hidden = ownProfile;
        container.innerHTML = ownProfile
            ? ""
            : "<button data-action=\"toggleProfileFollow\">" + (state.profileFollowed ? "取消关注" : "关注") + "</button>";
    }

    function toggleProfileFollow() {
        if (!state.profileUserId || String(state.profileUserId) === String(userId())) {
            return;
        }
        var operation;
        if (state.profileFollowed) {
            operation = request("/api/follows?followerId=" + userId() + "&followedId=" + state.profileUserId, {method: "DELETE"});
        } else {
            operation = request("/api/follows", {method: "POST", body: {followerId: userId(), followedId: Number(state.profileUserId)}});
        }
        operation.then(function () {
            notice(state.profileFollowed ? "已取消关注。" : "关注成功。");
            loadProfile();
        });
    }

    function loadProfileSocialLists(targetUserId) {
        Promise.all([
            request("/api/follows/followings?userId=" + targetUserId),
            request("/api/follows/followers?userId=" + targetUserId)
        ]).then(function (groups) {
            renderProfileUsers("followingList", groups[0] || []);
            renderProfileUsers("followerList", groups[1] || []);
        });
    }

    function renderProfileUsers(containerId, users) {
        renderList(containerId, users, function (item) {
            return "<div class=\"list-item profile-user-item\"><img class=\"mini-avatar\" src=\"" + img(item.avatar) +
                "\" alt=\"用户头像\"><div><strong>" + text(item.nickname || item.username) + "</strong>" +
                "<p>用户名：" + text(item.username) + "，用户ID：" + text(item.id) + "</p>" +
                "<a class=\"button-link secondary\" href=\"profile.html?userId=" + item.id + "\">查看主页</a></div></div>";
        });
    }

    function loadMyPosts() {
        loadOwnedContent("/api/posts/my", "我的帖子", "帖子");
    }

    function loadMyVideos() {
        loadOwnedContent("/api/videos/my", "我的短视频", "短视频");
    }

    function loadOwnedContent(path, title, typeName) {
        request(path).then(function (list) {
            var panel = byId("ownedContentPanel");
            if (panel) {
                panel.hidden = false;
            }
            byId("ownedContentTitle").textContent = title;
            renderList("ownedContentList", list || [], function (item) {
                var status = ownedContentStatus(item.status);
                return "<article class=\"list-item owned-content-item\">" +
                    "<img src=\"" + img(item.cover) + "\" alt=\"" + text(typeName) + "封面\">" +
                    "<div><strong>" + text(item.title) + "</strong>" +
                    "<p>话题：" + text(item.topic || "未选择") + "，发布时间：" + text(item.createTime) + "</p>" +
                    "<p>点赞 " + number(item.likeCount) + " / 收藏 " + number(item.collectCount) + " / 评论 " + number(item.commentCount) + "</p>" +
                    "<p>审核状态：" + status.audit + "，封禁状态：" + status.ban + "</p></div>" +
                    "</article>";
            });
            notice(title + "已加载。");
        });
    }

    function ownedContentStatus(status) {
        status = Number(status);
        if (status === 0) {
            return {audit: "待审核", ban: "未封禁"};
        }
        if (status === 1) {
            return {audit: "已通过", ban: "未封禁"};
        }
        if (status === 2) {
            return {audit: "未通过", ban: "未封禁"};
        }
        if (status === 3) {
            return {audit: "已通过", ban: "已封禁"};
        }
        return {audit: "未知", ban: "未知"};
    }

    function submitMerchantApplication(form) {
        var payload = values(form);
        request("/api/merchant/applications", {method: "POST", body: payload}).then(function () {
            form.reset();
            state.authUser.merchantApplyStatus = 1;
            saveAuthUser(state.authUser);
            notice("商户资格申请已提交，请等待管理员审核。");
            loadMerchantApplication();
        });
    }

    function loadMerchantApplication() {
        var form = byId("merchantApplicationForm");
        var link = byId("merchantApplicationLink");
        var statusNode = byId("merchantApplicationStatus");
        var panel = byId("merchantApplicationPanel");
        if (!statusNode) {
            return;
        }
        if (normalizeRole(state.authUser.role) === "ADMIN") {
            if (panel) {
                panel.hidden = true;
            }
            notice("管理员账号不需要申请商户资格。", true);
            return;
        }
        request("/api/merchant/applications/my").then(function (application) {
            var status = Number(state.authUser.merchantApplyStatus || 0);
            if (Number(state.authUser.isMerchant) === 1 || status === 2) {
                statusNode.textContent = "当前账号已获得商户资格，可以管理自己的门店、服务、排班和商品。";
                setMerchantApplicationEntryVisible(form, link, false);
                return;
            }
            if (status === 1 || (application && Number(application.status) === 0)) {
                statusNode.textContent = "商户资格正在审核中，请勿重复提交。";
                setMerchantApplicationEntryVisible(form, link, false);
                return;
            }
            if (status === 3 || (application && Number(application.status) === 2)) {
                statusNode.textContent = "上一次申请未通过，可以修改资料后重新提交。";
            } else {
                statusNode.textContent = form
                    ? "请完整填写商户基本信息后提交管理员审核。"
                    : "普通用户可以进入独立申请页提交商户资格申请。";
            }
            setMerchantApplicationEntryVisible(form, link, true);
        });
    }

    function setMerchantApplicationEntryVisible(form, link, visible) {
        if (form) {
            form.hidden = !visible;
        }
        if (link) {
            link.hidden = !visible;
        }
    }

    function updateStoreRegistrationEntry(ownProfile) {
        var link = byId("storeRegistrationLink");
        if (link) {
            link.hidden = !ownProfile || !isApprovedMerchant();
        }
    }

    function loadAdminData() {
        loadAdminUsers(state.adminUserFilterId);
        loadAdminApplications();
        loadApprovedPosts();
        loadAdminVideos();
        loadAdminComments();
        request("/api/statistics/dashboard").then(function (data) {
            var overview = data.overview || {};
            renderMetrics("adminMetrics", [
                ["用户", overview.userCount],
                ["帖子", overview.postCount],
                ["评论", overview.commentCount],
                ["预约", overview.appointmentCount],
                ["订单", overview.orderCount],
                ["成交额", money(overview.totalOrderAmount)]
            ]);
        });
        request("/api/admin/audit/posts?status=0").then(function (list) {
            renderRows("auditPostRows", list || [], function (post) {
                return "<tr><td>" + post.id + "</td><td><strong>" + text(post.title) + "</strong></td><td>" + postAuditStatus(post.status) + "</td>" +
                    "<td><button data-audit-detail=\"post\" data-audit-id=\"" + post.id + "\">审核</button></td></tr>";
            });
            bindAuditDetailButtons();
        });
        request("/api/admin/appointments").then(function (appointments) {
            request("/api/admin/product-orders").then(function (orders) {
                renderList("adminBusinessList", (appointments || []).slice(0, 6).map(function (item) {
                    return {title: "预约 #" + item.id, desc: "用户 " + item.userId + "，门店 " + item.storeId + "，状态：" + appointmentStatus(item.status)};
                }).concat((orders || []).slice(0, 6).map(function (item) {
                    return {title: "订单 " + item.orderNo, desc: "金额：" + money(item.totalAmount) + "，状态：" + orderStatus(item.status)};
                })), function (item) {
                    return "<div class=\"list-item\"><strong>" + item.title + "</strong><p>" + item.desc + "</p></div>";
                });
            });
        });
    }

    function searchAdminUsers(form) {
        loadAdminUsers(intVal(form.elements.userId.value));
    }

    function loadAllAdminUsers() {
        var form = byId("adminUserSearchForm");
        if (form) {
            form.reset();
        }
        loadAdminUsers(null);
    }

    function loadAdminUsers(id) {
        state.adminUserFilterId = id || null;
        var path = "/api/admin/users" + (id ? "?id=" + encodeURIComponent(id) : "");
        request(path).then(function (list) {
            renderRows("adminUserRows", list || [], function (user) {
                var admin = normalizeRole(user.role) === "ADMIN";
                var merchant = Number(user.isMerchant) === 1;
                var typeName = admin ? "管理员" : (merchant ? "商户" : "普通用户");
                var statusName = Number(user.status) === 1 ? "正常" : "已封禁";
                var operations = "系统管理员不可修改";
                if (!admin) {
                    operations = "<button data-admin-user-action=\"" + (merchant ? "set-normal" : "set-merchant") + "\" data-user-id=\"" + user.id + "\">" + (merchant ? "设为普通用户" : "设为商户") + "</button> " +
                        "<button class=\"" + (Number(user.status) === 1 ? "danger" : "secondary") + "\" data-admin-user-action=\"" + (Number(user.status) === 1 ? "disable" : "enable") + "\" data-user-id=\"" + user.id + "\">" + (Number(user.status) === 1 ? "封禁" : "解除封禁") + "</button>";
                }
                return "<tr><td>" + user.id + "</td><td>" + text(user.username) + "</td><td>" + text(user.nickname) + "</td>" +
                    "<td>" + typeName + "</td><td>" + statusName + "</td><td>" + text(user.createTime) + "</td><td>" + operations + "</td></tr>";
            });
            bindAdminUserButtons();
        });
    }

    function bindAdminUserButtons() {
        forEach(document.querySelectorAll("[data-admin-user-action]"), function (button) {
            button.onclick = function () {
                var id = button.getAttribute("data-user-id");
                var action = button.getAttribute("data-admin-user-action");
                request("/api/admin/users/" + id + "/" + action, {method: "PUT"}).then(function () {
                    notice("用户信息已更新。");
                    loadAdminUsers(state.adminUserFilterId);
                });
            };
        });
    }

    function bindAuditDetailButtons() {
        forEach(document.querySelectorAll("[data-audit-detail]"), function (button) {
            button.onclick = function () {
                var type = button.getAttribute("data-audit-detail");
                var id = button.getAttribute("data-audit-id");
                window.location.href = "admin-audit-detail.html?type=" + encodeURIComponent(type) + "&id=" + encodeURIComponent(id);
            };
        });
    }

    function initAdminAuditDetailPage() {
        var type = queryParam("type");
        var id = queryParam("id");
        if ((type !== "post" && type !== "video") || !id) {
            notice("审核内容参数不完整。", true);
            return;
        }
        var detailPath = type === "video"
            ? "/api/admin/audit/videos/" + id
            : "/api/admin/audit/posts/" + id;
        request(detailPath).then(function (item) {
            renderAdminAuditDetail(item, type);
            bindAuditDecisionButtons(type, id, item.status);
            notice(type === "video" ? "短视频审核内容已加载。" : "帖子审核内容已加载。");
        });
    }

    function renderAdminAuditDetail(item, type) {
        var node = byId("auditContentDetail");
        var titleNode = byId("auditPageTitle");
        if (!node) {
            return;
        }
        if (titleNode) {
            titleNode.textContent = type === "video" ? "短视频审核" : "帖子审核";
        }
        var statusHtml = type === "video" ? videoAuditStatus(item.status) : postAuditStatus(item.status);
        var meta = "<div class=\"audit-meta\"><span>ID：" + text(item.id) + "</span><span>发布用户：" + text(item.userId) +
            "</span><span>话题：" + text(item.topic) + "</span><span>发布时间：" + text(item.createTime) +
            "</span><span>状态：" + statusHtml + "</span></div>";
        if (type === "video") {
            node.innerHTML = '<article class="video-detail-content audit-content-detail">' +
                '<video controls preload="metadata" poster="' + text(img(item.cover)) + '" src="' + text(resourceUrl(item.videoUrl)) + '"></video>' +
                '<div class="video-detail-copy"><h2>' + text(item.title) + '</h2><p>' + text(item.content) + '</p>' + meta + '</div></article>';
            return;
        }
        node.innerHTML = "<article class=\"post-detail-content audit-content-detail\">" +
            "<img class=\"post-detail-cover\" src=\"" + text(img(item.cover)) + "\" alt=\"帖子图片\">" +
            "<div><h2>" + text(item.title) + "</h2><p>" + text(item.content) + "</p>" + meta + "</div></article>";
    }

    function bindAuditDecisionButtons(type, id, status) {
        var form = byId("auditDecisionForm");
        if (!form) {
            return;
        }
        if (Number(status) !== 0) {
            form.hidden = true;
            notice("该内容已经完成审核。", true);
            return;
        }
        forEach(form.querySelectorAll("[data-audit-status]"), function (button) {
            button.onclick = function () {
                var reviewStatus = intVal(button.getAttribute("data-audit-status"));
                var reviewPath = type === "video"
                    ? "/api/admin/audit/videos/" + id + "/review"
                    : "/api/admin/audit/posts/" + id + "/review";
                request(reviewPath, {
                    method: "PUT",
                    body: {status: reviewStatus, operatorId: userId(), remark: form.elements.remark.value.trim()}
                }).then(function () {
                    notice(reviewStatus === 1 ? "审核已通过。" : "审核已驳回。");
                    window.setTimeout(function () {
                        window.location.href = "admin.html";
                    }, 500);
                });
            };
        });
    }

    function loadAdminApplications() {
        request("/api/admin/merchant-applications?status=0").then(function (list) {
            renderRows("merchantApplicationRows", list || [], function (item) {
                var attachment = item.attachmentUrl
                    ? "<a class=\"button-link secondary\" href=\"" + text(resourceUrl(item.attachmentUrl)) + "\" target=\"_blank\" rel=\"noopener\">" + text(item.attachmentName || "查看附件") + "</a>"
                    : "无";
                return "<tr><td>" + item.id + "</td><td>" + text(item.userId) + "</td><td>" + text(item.merchantName) + "</td>" +
                    "<td>" + text(item.contactName) + "</td><td>" + text(item.contactPhone) + "</td><td>" + text(item.businessType) + "</td>" +
                    "<td>" + text(item.businessAddress) + "<br>" + text(item.reason) + "</td>" +
                    "<td>" + attachment + "</td>" +
                    "<td><button data-merchant-review=\"" + item.id + "\" data-status=\"1\">通过</button> " +
                    "<button class=\"danger\" data-merchant-review=\"" + item.id + "\" data-status=\"2\">驳回</button></td></tr>";
            });
            bindMerchantReviewButtons();
        });
        request("/api/admin/product-applications?status=0").then(function (list) {
            renderRows("productApplicationRows", list || [], function (item) {
                return "<tr><td>" + item.id + "</td><td>" + text(item.merchantUserId) + "</td><td>" + text(item.operationType) + "</td>" +
                    "<td>" + text(item.productName || item.productId) + "</td>" +
                    "<td><button data-product-review=\"" + item.id + "\" data-status=\"1\">通过</button> " +
                    "<button class=\"danger\" data-product-review=\"" + item.id + "\" data-status=\"2\">驳回</button></td></tr>";
            });
            bindProductReviewButtons();
        });
    }

    function bindMerchantReviewButtons() {
        forEach(document.querySelectorAll("[data-merchant-review]"), function (button) {
            button.onclick = function () {
                request("/api/admin/merchant-applications/" + button.getAttribute("data-merchant-review") + "/review", {
                    method: "PUT",
                    body: {status: intVal(button.getAttribute("data-status")), remark: "管理员审核"}
                }).then(function () {
                    notice("商户资格审核已完成。");
                    loadAdminApplications();
                });
            };
        });
    }

    function bindProductReviewButtons() {
        forEach(document.querySelectorAll("[data-product-review]"), function (button) {
            button.onclick = function () {
                request("/api/admin/product-applications/" + button.getAttribute("data-product-review") + "/review", {
                    method: "PUT",
                    body: {status: intVal(button.getAttribute("data-status")), remark: "管理员审核"}
                }).then(function () {
                    notice("商品操作审核已完成。");
                    loadAdminApplications();
                });
            };
        });
    }

    function loadAdminVideos() {
        Promise.all([0, 1, 2].map(function (status) {
            return request("/api/admin/audit/videos?status=" + status);
        })).then(function (groups) {
            var videos = [];
            forEach(groups, function (group) {
                videos = videos.concat(group || []);
            });
            renderRows("auditVideoRows", videos, function (video) {
                var status = Number(video.status);
                var operations = status === 0
                    ? '<button data-audit-detail="video" data-audit-id="' + video.id + '">审核</button>'
                    : status === 1
                        ? '<button class="danger" data-admin-video-action="delete" data-video-id="' + video.id + '">删除</button>'
                        : '<button data-admin-video-action="restore" data-video-id="' + video.id + '">恢复</button>';
                return '<tr><td>' + video.id + '</td><td><strong>' + text(video.title) + '</strong></td>' +
                    '<td>' + text(video.userId) + '</td><td>' + videoAuditStatus(video.status) + '</td><td>' + operations + '</td></tr>';
            });
            bindAuditDetailButtons();
            bindAdminVideoButtons();
        });
    }

    function bindAdminVideoButtons() {
        forEach(document.querySelectorAll("[data-admin-video-action]"), function (button) {
            button.onclick = function () {
                var videoId = button.getAttribute("data-video-id");
                var action = button.getAttribute("data-admin-video-action");
                var requestOptions = {method: "PUT"};
                var path;
                var message;
                if (action === "delete") {
                    path = "/api/admin/videos/" + videoId + "/delete";
                    message = "短视频已删除。";
                } else {
                    path = "/api/admin/videos/" + videoId + "/restore";
                    message = "短视频已恢复。";
                }
                request(path, requestOptions).then(function () {
                    notice(message);
                    loadAdminVideos();
                });
            };
        });
    }

    function loadApprovedPosts() {
        request("/api/admin/audit/posts?status=1").then(function (list) {
            renderRows("approvedPostRows", list || [], function (post) {
                var pinAction = Number(post.isPinned) === 1 ? "unpin" : "pin";
                var featureAction = Number(post.isFeatured) === 1 ? "unfeature" : "feature";
                return "<tr><td>" + post.id + "</td><td><strong>" + text(post.title) + "</strong><br>" + text(post.content) + "</td>" +
                    "<td>" + (Number(post.isPinned) === 1 ? "是" : "否") + "</td>" +
                    "<td>" + (Number(post.isFeatured) === 1 ? "是" : "否") + "</td>" +
                    "<td><button data-post-operation=\"" + post.id + "\" data-operation=\"" + pinAction + "\">" + (pinAction === "pin" ? "置顶" : "取消置顶") + "</button> " +
                    "<button data-post-operation=\"" + post.id + "\" data-operation=\"" + featureAction + "\">" + (featureAction === "feature" ? "加精" : "取消加精") + "</button></td></tr>";
            });
            bindPostOperationButtons();
        });
    }

    function bindPostOperationButtons() {
        forEach(document.querySelectorAll("[data-post-operation]"), function (button) {
            button.onclick = function () {
                var postId = button.getAttribute("data-post-operation");
                var operation = button.getAttribute("data-operation");
                request("/api/admin/posts/" + postId + "/" + operation, {method: "PUT"}).then(function () {
                    notice("帖子运营状态已更新。");
                    loadApprovedPosts();
                });
            };
        });
    }

    function scrollToAdminSection(sectionId) {
        var section = byId(sectionId);
        if (section) {
            section.scrollIntoView({behavior: "smooth", block: "start"});
        }
    }

    function loadAdminTopics(trigger) {
        if (trigger) {
            scrollToAdminSection("topicManagementSection");
        }
        request("/api/admin/audit/topics?status=0").then(function (list) {
            renderRows("auditTopicRows", list || [], function (topic) {
                return "<tr>" +
                    "<td>" + topic.id + "</td>" +
                    "<td><strong>#" + text(topic.name) + "</strong><br>" + text(topic.description) + "</td>" +
                    "<td><input class=\"table-input\" data-topic-sort=\"" + topic.id + "\" type=\"number\" value=\"" + number(topic.sort) + "\" placeholder=\"排序值\"></td>" +
                    "<td>" + topicAuditStatus(topic.status) + "</td>" +
                    "<td><button data-review-topic=\"" + topic.id + "\" data-status=\"1\">通过</button> <button class=\"danger\" data-review-topic=\"" + topic.id + "\" data-status=\"2\">驳回</button></td>" +
                    "</tr>";
            });
            bindTopicReviewButtons();
        });

        request("/api/topics").then(function (list) {
            renderList("adminTopicList", list || [], function (topic) {
                return "<div class=\"list-item\">" +
                    "<strong>#" + text(topic.name) + "</strong>" +
                    "<p>排序值：" + number(topic.sort) + "，帖子数：" + number(topic.postCount) + "</p>" +
                    "<p>" + text(topic.description) + "</p>" +
                    "<div class=\"card-actions\"><button class=\"danger\" data-delete-topic=\"" + topic.id + "\">删除</button></div>" +
                    "</div>";
            });
            bindTopicDeleteButtons();
        });
    }

    function createAdminTopic(form) {
        var payload = values(form);
        payload.sort = intVal(payload.sort) || 0;
        request("/api/admin/topics", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("话题已添加并展示。");
            loadAdminTopics();
            loadTopics();
        });
    }

    function bindTopicReviewButtons() {
        var buttons = document.querySelectorAll("[data-review-topic]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                var topicId = button.getAttribute("data-review-topic");
                var status = intVal(button.getAttribute("data-status"));
                var sortInput = document.querySelector("[data-topic-sort='" + topicId + "']");
                var sort = sortInput ? intVal(sortInput.value) || 0 : 0;
                request("/api/admin/audit/topics/" + topicId + "/review", {
                    method: "PUT",
                    body: {status: status, sort: sort, operatorId: userId(), remark: "前端审核"}
                }).then(function () {
                    notice(status === 1 ? "话题审核通过。" : "话题已驳回。");
                    loadAdminTopics();
                    loadTopics();
                });
            };
        });
    }

    function bindTopicDeleteButtons() {
        var buttons = document.querySelectorAll("[data-delete-topic]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                if (!confirm("确定删除这个话题吗？")) {
                    return;
                }
                request("/api/admin/topics/" + button.getAttribute("data-delete-topic"), {method: "DELETE"}).then(function () {
                    notice("话题已删除。");
                    loadAdminTopics();
                    loadTopics();
                });
            };
        });
    }

    function createSensitiveWord(form) {
        var payload = values(form);
        payload.level = intVal(payload.level);
        request("/api/sensitive-words", {method: "POST", body: payload}).then(function () {
            form.reset();
            notice("敏感词已添加。");
            loadSensitiveWords();
        });
    }

    function loadSensitiveWords(trigger) {
        if (trigger) {
            scrollToAdminSection("sensitiveWordSection");
        }
        request("/api/sensitive-words").then(function (list) {
            renderList("sensitiveWordList", list || [], function (item) {
                return "<div class=\"list-item\"><strong>" + text(item.word) + "</strong><p>级别：" + text(item.level) + "，状态：" + statusLabel(item.status) + "</p></div>";
            });
        });
    }

    function openAdminComments() {
        scrollToAdminSection("commentManagementSection");
        loadAdminComments();
    }

    function loadAdminComments() {
        request("/api/admin/audit/comments").then(function (list) {
            renderRows("adminCommentRows", list || [], function (item) {
                var visible = Number(item.status) === 1;
                return "<tr><td>" + text(item.id) + "</td><td>" + text(item.postId) + "</td><td>" + text(item.userId) +
                    "</td><td>" + text(item.content) + "</td><td>" + (visible ? "正常" : "已隐藏") + "</td>" +
                    "<td><button class=\"" + (visible ? "danger" : "secondary") + "\" data-admin-comment-action=\"" +
                    (visible ? "hide" : "restore") + "\" data-comment-id=\"" + item.id + "\">" +
                    (visible ? "隐藏" : "恢复") + "</button></td></tr>";
            });
            bindAdminCommentButtons();
        });
    }

    function bindAdminCommentButtons() {
        forEach(document.querySelectorAll("[data-admin-comment-action]"), function (button) {
            button.onclick = function () {
                var id = button.getAttribute("data-comment-id");
                var action = button.getAttribute("data-admin-comment-action");
                request("/api/admin/audit/comments/" + id + "/" + action, {method: "PUT"}).then(function () {
                    notice(action === "hide" ? "评论已隐藏。" : "评论已恢复。");
                    loadAdminComments();
                });
            };
        });
    }

    function loadReports(trigger) {
        if (trigger) {
            scrollToAdminSection("reportSection");
        }
        request("/api/reports").then(function (list) {
            renderList("reportList", list || [], function (item) {
                var actions = Number(item.status) === 0
                    ? "<div class=\"card-actions\"><button data-report-handle=\"1\" data-report-id=\"" + item.id + "\">举报成立</button> " +
                        "<button class=\"secondary\" data-report-handle=\"2\" data-report-id=\"" + item.id + "\">驳回举报</button></div>"
                    : "<p>处理结果：" + text(item.handleResult || "无") + "</p>";
                return "<div class=\"list-item\"><strong>举报 " + text(item.targetType) + "#" + text(item.targetId) +
                    "</strong><p>举报用户：" + text(item.userId) + "</p><p>原因：" + text(item.reason) +
                    "</p><p>状态：" + reportStatus(item.status) + "</p>" + actions + "</div>";
            });
            bindReportHandleButtons();
        });
    }

    function bindReportHandleButtons() {
        forEach(document.querySelectorAll("[data-report-handle]"), function (button) {
            button.onclick = function () {
                var handleResult = prompt("请输入处理说明");
                if (!handleResult || !handleResult.trim()) {
                    return;
                }
                var id = button.getAttribute("data-report-id");
                var status = intVal(button.getAttribute("data-report-handle"));
                request("/api/reports/" + id + "/handle", {
                    method: "PUT",
                    body: {status: status, handleResult: handleResult.trim()}
                }).then(function () {
                    notice(status === 1 ? "举报已确认成立。" : "举报已驳回。");
                    loadReports();
                    loadAdminComments();
                    loadApprovedPosts();
                });
            };
        });
    }

    function report(targetType, targetId) {
        var reason = prompt("请输入举报原因");
        if (!reason) {
            return;
        }
        request("/api/reports", {method: "POST", body: {targetType: targetType, targetId: targetId, userId: userId(), reason: reason}}).then(function () {
            notice("举报已提交。");
        });
    }

    function uploadPostCover() {
        uploadFile("postCoverFile", "post").then(function (file) {
            byId("postForm").elements.cover.value = file.url;
        });
    }

    function uploadPetAvatar() {
        uploadFile("petAvatarFile", "pet").then(function (file) {
            byId("petForm").elements.avatar.value = file.url;
        });
    }

    function uploadStoreCover() {
        uploadFile("storeCoverFile", "store").then(function (file) {
            byId("storeForm").elements.cover.value = file.url;
        });
    }

    function uploadServiceItemCover() {
        uploadFile("serviceItemCoverFile", "service").then(function (file) {
            byId("serviceItemForm").elements.cover.value = file.url;
        });
    }

    function uploadProductCover() {
        uploadFile("productCoverFile", "product").then(function (file) {
            if (byId("productForm")) {
                byId("productForm").elements.cover.value = file.url;
            }
        });
    }

    function uploadMerchantProductCover() {
        uploadFile("merchantProductCoverFile", "product").then(function (file) {
            byId("merchantProductCreateForm").elements.cover.value = file.url;
        });
    }

    function uploadMerchantApplicationAttachment() {
        var form = byId("merchantApplicationForm");
        uploadFile("merchantApplicationAttachmentFile", "merchant-application").then(function (file) {
            form.elements.attachmentName.value = file.originalName;
            form.elements.attachmentUrl.value = file.url;
        });
    }

    function uploadFile(inputId, module) {
        var input = byId(inputId);
        if (!input || !input.files || !input.files[0]) {
            notice("请选择文件。", true);
            return Promise.reject(new Error("请选择文件"));
        }
        var formData = new FormData();
        formData.append("file", input.files[0]);
        formData.append("module", module);
        return request("/api/files/upload", {method: "POST", body: formData}).then(function (file) {
            notice("文件已上传。");
            return file;
        });
    }

    function renderMetrics(id, list) {
        var node = byId(id);
        if (!node) {
            return;
        }
        node.innerHTML = list.map(function (item) {
            return "<div class=\"metric\"><span>" + item[0] + "</span><strong>" + text(item[1]) + "</strong></div>";
        }).join("");
    }

    function renderCards(id, list, renderer) {
        var node = byId(id);
        if (!node) {
            return;
        }
        list = list || [];
        node.innerHTML = list.length ? list.map(renderer).join("") : emptyHtml();
        bindActionCallbacks(id);
    }

    var callbackStore = {};
    var callbackId = 0;

    function card(image, title, lines, actions, href) {
        var buttons = "";
        if (actions) {
            buttons = "<div class=\"card-actions\">" + actions.map(function (action) {
                var id = "cb_" + (++callbackId);
                callbackStore[id] = action[1];
                return "<button class=\"" + (action[2] || "") + "\" data-callback=\"" + id + "\">" + action[0] + "</button>";
            }).join("") + "</div>";
        }
        var content = "<img src=\"" + img(image) + "\" alt=\"图片\"><div class=\"card-body\"><h3>" + text(title) + "</h3>" + lines.map(function (line) {
            return "<p>" + text(line) + "</p>";
        }).join("") + "</div>";
        if (href) {
            return "<article class=\"card\"><a class=\"card-main-link\" href=\"" + text(href) + "\">" + content + "</a><div class=\"card-body card-action-body\">" + buttons + "</div></article>";
        }
        return "<article class=\"card\">" + content.replace("</div>", buttons + "</div>") + "</article>";
    }

    function bindActionCallbacks(scopeId) {
        var scope = byId(scopeId);
        if (!scope) {
            return;
        }
        var buttons = scope.querySelectorAll("[data-callback]");
        forEach(buttons, function (button) {
            button.onclick = function () {
                callbackStore[button.getAttribute("data-callback")]();
            };
        });
    }

    function renderList(id, list, renderer) {
        var node = byId(id);
        if (!node) {
            return;
        }
        list = list || [];
        node.innerHTML = list.length ? list.map(renderer).join("") : emptyHtml();
    }

    function renderRows(id, list, renderer) {
        var node = byId(id);
        if (!node) {
            return;
        }
        list = list || [];
        node.innerHTML = list.length ? list.map(renderer).join("") : "<tr><td colspan=\"8\">暂无数据</td></tr>";
    }

    function values(form) {
        var data = {};
        forEach(form.elements, function (el) {
            if (!el.name) {
                return;
            }
            data[el.name] = el.value === "" ? null : el.value;
        });
        return data;
    }

    function byId(id) {
        return document.getElementById(id);
    }

    function forEach(list, fn) {
        if (!list) {
            return;
        }
        Array.prototype.forEach.call(list, fn);
    }

    function findActionTarget(node) {
        while (node && node !== document.body) {
            if (node.getAttribute && node.getAttribute("data-action")) {
                return node;
            }
            node = node.parentNode;
        }
        return null;
    }

    function notice(message, error) {
        var node = byId("notice");
        if (!node) {
            return;
        }
        node.textContent = message;
        node.classList.toggle("error", !!error);
    }

    function userId() {
        return intVal(state.authUser && state.authUser.id ? state.authUser.id : localStorage.getItem("pet_user_id") || "1");
    }

    function intVal(value) {
        if (value === null || value === "" || value === undefined) {
            return null;
        }
        return parseInt(value, 10);
    }

    function decimalVal(value) {
        if (value === null || value === "" || value === undefined) {
            return null;
        }
        return parseFloat(value);
    }

    function number(value) {
        return value === null || value === undefined ? 0 : value;
    }

    function enabledItems(list) {
        return (list || []).filter(function (item) {
            return item.status === null || item.status === undefined || Number(item.status) === 1;
        });
    }

    function rankItems(list, scoreGetter, limit) {
        return (list || []).slice().sort(function (left, right) {
            return Number(scoreGetter(right) || 0) - Number(scoreGetter(left) || 0);
        }).slice(0, limit);
    }

    function text(value) {
        if (value === null || value === undefined || value === "") {
            return "-";
        }
        return String(value).replace(/[&<>"']/g, function (char) {
            return {"&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"}[char];
        });
    }

    function money(value) {
        if (value === null || value === undefined || value === "") {
            return "0.00";
        }
        return Number(value).toFixed(2);
    }

    function img(url) {
        url = url || "assets/images/content/default-post.jpg";
        if (!url) {
            return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent("<svg xmlns='http://www.w3.org/2000/svg' width='360' height='200'><rect width='360' height='200' fill='#dce4e8'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' fill='#637381' font-size='20'>暂无图片</text></svg>");
        }
        if (url.indexOf("http") === 0 || url.indexOf("data:") === 0) {
            return url;
        }
        if (url.indexOf("assets/") === 0) {
            return url;
        }
        return state.apiBase + url;
    }

    function resourceUrl(url) {
        if (!url || url.indexOf("http") === 0 || url.indexOf("data:") === 0) {
            return url || "";
        }
        return state.apiBase + url;
    }

    function yesNo(value) {
        return Number(value) === 1 ? "是" : "否";
    }

    function emptyHtml() {
        return "<div class=\"empty\">暂无数据</div>";
    }

    function trimSlash(value) {
        return (value || "").replace(/\/+$/, "");
    }

    function statusLabel(status) {
        return Number(status) === 1 ? "<span class=\"status\">启用</span>" : "<span class=\"status off\">停用</span>";
    }

    function postAuditStatus(status) {
        var map = {
            0: "<span class=\"status waiting\">待审核</span>",
            1: "<span class=\"status\">已通过</span>",
            2: "<span class=\"status off\">已驳回</span>"
        };
        return map[Number(status)] || "<span class=\"status off\">未知</span>";
    }

    function videoAuditStatus(status) {
        var map = {
            0: '<span class="status waiting">待审核</span>',
            1: '<span class="status">已通过</span>',
            2: '<span class="status off">已删除</span>'
        };
        return map[Number(status)] || '<span class="status off">未知</span>';
    }

    function topicAuditStatus(status) {
        var map = {
            0: "<span class=\"status waiting\">待审核</span>",
            1: "<span class=\"status\">已通过</span>",
            2: "<span class=\"status off\">已删除/驳回</span>"
        };
        return map[Number(status)] || "<span class=\"status off\">未知</span>";
    }

    function productApplicationStatus(status) {
        return {0: "待审核", 1: "已通过", 2: "已驳回"}[Number(status)] || "未知";
    }

    function appointmentStatus(status) {
        return {0: "已取消/驳回", 1: "已预约", 2: "已完成", 3: "待商户确认"}[status] || "未知";
    }

    function orderStatus(status) {
        return {0: "已取消", 1: "已下单", 2: "已完成"}[status] || "未知";
    }

    function reportStatus(status) {
        return {0: "待处理", 1: "已处理", 2: "已驳回"}[status] || "未知";
    }

    function stat(label, value) {
        return "<div class=\"profile-stat\"><span>" + label + "</span><strong>" + text(value) + "</strong></div>";
    }

    function findById(list, id) {
        id = String(id);
        for (var i = 0; i < list.length; i++) {
            if (String(list[i].id) === id) {
                return list[i];
            }
        }
        return {};
    }

    function queryParam(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    function normalizeDateTime(value) {
        return value ? value + ":00" : null;
    }

    function toDateTimeLocal(date, time) {
        if (!date || !time) {
            return "";
        }
        return date + "T" + String(time).slice(0, 5);
    }

    function normalizeRole(role) {
        return String(role || "").toUpperCase();
    }

    function readAuthUser() {
        var raw = localStorage.getItem("pet_auth_user");
        if (!raw) {
            return null;
        }
        try {
            return JSON.parse(raw);
        } catch (error) {
            localStorage.removeItem("pet_auth_user");
            return null;
        }
    }

    function saveAuthUser(user) {
        state.authUser = user || null;
        localStorage.setItem("pet_auth_user", JSON.stringify(state.authUser));
        if (state.authUser && state.authUser.id) {
            localStorage.setItem("pet_user_id", state.authUser.id);
        }
    }

    function clearAuthUser() {
        state.authUser = null;
        localStorage.removeItem("pet_auth_user");
    }

    function saveApiBase(value) {
        state.apiBase = trimSlash(value || "http://localhost:8082");
        localStorage.setItem("pet_api_base", state.apiBase);
    }

    function saveApiBaseFromPage() {
        var input = byId("apiBase");
        if (input && input.value) {
            saveApiBase(input.value);
        }
    }
})();
