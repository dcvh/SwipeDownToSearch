# SwipeDownToSearch
An attempt to replicate the search layout in [Kik application](https://play.google.com/store/apps/details?id=kik.android&hl=en)

This is only a Proof of Concept, **_not a usable library_**. 

## Approach
The main idea is to place the first view in front of the second view. When user slides down the first view (front view), the layout moves the toolbar and second view (rear view) up, and the front view down simultaneously.  
We use ConstraintLayout in order to gain the ability to overlap views. For more intuitive usage, a custom view is written, which extends the ConstraintLayout.

Please note that this approach is definitely **_not what Kik use in their application_**, it is another method to imitate the look of Kik.

## Prerequisites 
1. This layout must be used as a separated layout taking up the whole screen, in other words both height and width attributes should be assigned match_parent value.
2. This layout can only contain 3 three child views, otherwise it will throw an exception. The order of child views must be as follows: the toolbar, the rear view, then the front view.
3. The width/height attributes of child views (except for the toolbar height) must be assigned 0dp, otherwise the layout might not render properly.

All constraint attributes will be programmatically initialized so it is not necessary to define those ones in xml layout.
