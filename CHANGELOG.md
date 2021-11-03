## CHANGELOG

### 0.2.0 (2021-11-01)

#### Bugs
* Fix #66: Removed code smells and bugs
* Fix #64: Allow concurrent access to Map preserving insertion order
* Fix #57: Update expired SSL certificates (new expiry date 2031-11-01)

#### Improvements
* Fix #59: adding a getter to access namespace from the path (fabric8/kubernetes-client#2921)
* Fix #66: Added getLastRequest method to MockServer
* Fix #66: CRUD dispatcher handles PUT independently of POST

#### Dependency Upgrade
* Fix #52: Updated OkHttpClient to version 3.12.12
* Fix #65: Updated Jackson to version 2.13.0
* Fix #61: Removed Sundrio dependency

#### New Feature

### 0.1.8 (2020-04-14)
#### Bugs
#### Improvements
#### Dependency Upgrade
* Updated OkHttpClient to version 3.12.6

#### New Feature

### 0.1.7 (2019-09-27)
#### Bugs

#### Improvements
* AttributeSet now examines all attributes when determining if another AttributeSet matches.
#### Dependency Upgrade

#### New Feature


### 0.1.6 (26-09-2019)
#### Bugs

#### Improvements
* Attribute Type matching now supports EXISTS and NOT_EXISTS

#### Dependency Upgrade

#### New Feature


### 0.1.4 (16-09-2019)
#### Bugs

#### Improvements
* Adding argument to getStatusCode

#### Dependency Upgrade

#### New Feature



### 0.1.3 (03-09-2019)
#### Bugs

#### Improvements
  * Added Attribute Types (WITH and WITHOUT) to works with withoutLabel filter (AttributeSet.matches()).

#### Dependency Upgrade

#### New Feature

