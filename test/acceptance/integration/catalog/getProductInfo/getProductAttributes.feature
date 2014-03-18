# -*- coding: utf-8 -*-
Feature: Get attributes of a product in the catalogue
    As a fi-ware user
    I want to be able to get attributes of a product in the catalogue
    so that they become more functional and useful

    @happy_path
    Scenario: Get attributes of a product in the catalogue
        Given The "Product_test_0001" has been created and the sdc is up and properly configured
        When I get attributes of a product "Product_test_0001" in the catalog
        Then I receive an "OK" response with a "Product attributes" with some items

    Scenario: Try to get attributes of a product that does no exist in the catalogue
        Given the sdc is up and properly configured
        When I get attributes of a product "gfhfgh" in the catalog
        Then I receive an "Internal Server Error" response with an "Product does not exist" error messages

    Scenario: Cause an Not Found path error when get attributes of a product in the catalogue
        Given The "Product_test_0001" has been created and the sdc is up and properly configured
        When I request a wrong path when get attributes of a product "Product_test_0001" in the catalogue
        Then I receive an "Not Found" response with an "exception" error messages

    Scenario Outline: launch un bad method error get attributes of a product in the catalogue
        Given The "Product_test_0001" has been created and the sdc is up and properly configured
        When I try to get attributes of a product "Product_test_0001" in the catalog with request method is "<method>"
            |method|
            |<method>|
        Then I receive an "bad Method" response with an "exception" error messages
    Examples:
        | method |
        |PUT|
        |POST|