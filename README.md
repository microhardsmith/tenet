![logo](https://github.com/microhardsmith/tenet-lib/blob/master/tenet.png)

# Tenet Project Home

This is the home page of the Tenet Project

# Description

Tenet is designed as several components that help you build microservices, which aims at simplicity and performance.

The tenet of the Tenet project are:

* All the components should stay as simple as possible, simplicity is our primary goal, The codebase should be kept at a moderate size, and the error stack traces as well as class inheritance relationships should also be maintained accordingly.
* It's recommend that any developers who want to use Tenet project in their application read its source code first. The code should exhibit a relatively high level of readability, allowing all developers to easily grasp the author's intended ideas.
* Performance would not be our first concern if it's conflicting the first two points. If there is a simple way to achieve significant performance boost, we will adopt it. However, if obtaining marginal gains requires a plethora of magic, such as bytecode generation, unsafe calls, and so forth, we prioritize simplicity.
* We don't aim to solve the majority of issues in backend development; instead, we encourage developers to customize the functionality they desire based on the source code.

# How to use

Tenet application need both libraries from Maven repositories, but also need to load some native libraries in [tenet-lib](https://github.com/microhardsmith/tenet-lib)
To run tenet application, you need to specify system properties :

```shell
--enable-preview
--enable-native-access=ALL-UNNAMED
-DTENET_LIBRARY_PATH=/path/to/lib
```

Visit [tenet-lib](https://github.com/microhardsmith/tenet-lib) for more information.

# Status

The project is currently in the development stage, and we do not plan to make a full release until many Java preview features it relies on become production-ready. There are still numerous features to be implemented, and we are closely monitoring how Project Valhalla will impact the Java ecosystem. If you are interested in the Tenet project, feel free to reach out and get involved in the development.

For now, the networking model has been implemented and there shouldn't be major changes in the future versions, you can get more information about it in [network-model](https://github.com/microhardsmith/tenet/blob/master/network-model.md)

# Contact

If you have any questions, suggestions, or would like to contribute to this project, feel free to reach out to me:

- Email: benrush0705@gmail.com

Looking forward to engaging with you and collaboratively advancing the project!

