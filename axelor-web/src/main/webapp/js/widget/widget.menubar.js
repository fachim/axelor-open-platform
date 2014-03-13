/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
(function() {

var module = angular.module('axelor.ui');

MenuBarCtrl.$inject = ['$scope', '$element'];
function MenuBarCtrl($scope, $element) {

	this.isDivider = function(item) {
		return !item.title && !item.icon;
	};
	
	$scope.isImage = function (menu) {
		return menu.icon && menu.icon.indexOf('fa-') !== 0;
	};
	
	$scope.isIcon = function (menu) {
		return menu.icon && menu.icon.indexOf('fa-') === 0;
	};
	
	$scope.canShowTitle = function(menu) {
		return menu.showTitle === null || menu.showTitle === undefined || menu.showTitle;
	};
}

module.directive('uiMenuBar', function() {

	return {
		replace: true,
		controller: MenuBarCtrl,
		scope: {
			menus: '=',
			handler: '='
		},
		link: function(scope, element, attrs, ctrl) {

			ctrl.handler = scope.handler;
			
			scope.onMenuClick = _.once(function onMenuClick() {
				element.find('.dropdown-toggle').dropdown('toggle');
			});
		},

		template:
			"<ul class='nav menu-bar'>" +
				"<li class='menu dropdown' ng-class='{\"button-menu\": menu.isButton}' ng-repeat='menu in menus'>" +
					"<a href='' class='dropdown-toggle' ng-class='{\"btn\": menu.isButton}' data-toggle='dropdown' ng-click='onMenuClick()'>" +
						"<img ng-if='isImage(menu)' ng-src='{{menu.icon}}'> " +
						"<i class='fa {{menu.icon}}' ng-if='isIcon(menu)'></i> " +
						"<span ng-show='canShowTitle(menu)'>{{menu.title}}</span> " +
						"<b class='caret'></b>" +
					"</a>" +
					"<ul ui-menu='menu'></ul>" +
				"</li>" +
			"</ul>"
	};
});

module.directive('uiMenu', function() {

	return {
		replace: true,
		require: '^uiMenuBar',
		scope: {
			menu: '=uiMenu'
		},
		link: function(scope, element, attrs, ctrl) {

		},
		template:
			"<ul class='dropdown-menu'>" +
				"<li ng-repeat='item in menu.items' ui-menu-item='item'>" +
			"</ul>"
	};
});

module.directive('uiMenuItem', ['ActionService', function(ActionService) {

	return {
		replace: true,
		require: '^uiMenuBar',
		scope: {
			item: '=uiMenuItem'
		},
		link: function(scope, element, attrs, ctrl) {

			var item = scope.item;
			var handler = null;

			scope.field  = item;
			scope.isDivider = ctrl.isDivider(item);

			if (item.action) {
				handler = ActionService.handler(ctrl.handler, element, {
					action: item.action
				});
			}

			scope.isRequired = function(){};
			scope.isValid = function(){};

			var attrs = {
				hidden: false,
				readonly: false
			};

			scope.attr = function(name, value) {
				attrs[name] = value;
			};

			scope.isReadonly = function(){
				if (attrs.readonly) return true;
				if (_.isFunction(item.active)) {
					return !item.active();
				}
				return false;
			};

			scope.isHidden = function(){
				return attrs.hidden;
			};

			var form = element.parents('.form-view:first');
			var formScope = form.data('$scope');

			if (formScope) {
				formScope.$watch('record', function(rec) {
					scope.record = rec;
				});
			}

			scope.onClick = function(e) {
				$(e.srcElement).parents('.dropdown').dropdown('toggle');
				if (item.action) {
					return handler.onClick();
				}
				if (_.isFunction(item.click)) {
					return item.click(e);
				}
			};

			scope.cssClass = function() {
				if (scope.isDivider) {
					return 'divider';
				}
			};
		},
		template:
			"<li ng-class='cssClass()' ui-widget-states	ng-show='!isHidden()'>" +
				"<a href='' ng-show='isReadonly()' class='disabled'>{{item.title}}</a>" +
				"<a href='' ng-show='!isDivider && !isReadonly()' ng-click='onClick($event)'>{{item.title}}</a>" +
			"</li>"
	};
}]);

}).call(this);
