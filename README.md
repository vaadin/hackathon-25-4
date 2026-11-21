# hackathon-25-4
Now upgraded to 25.4. Most changes have been made with Vaadin Copilot, although I cleaned up the `AdminNav` changes because they annoyed me, added the `AdminNav` to the new view manually because Copilot didn't understand the request, and fixed the routing manually, because Copilot got it wrong and `AdminNav` didn't compile anymore.

I had quite a few issues in creating a new button to the nav layout and in creating the view, although those weren't actually what I was even planning to test -- I just wanted an empty view so that I could test the Auto-responsive FormLayout and Optimistic Editing without interference from previous content.

I commented along my testing (about what I did and the issues I faced) in [Slack](https://vaadin.slack.com/archives/C0CFXNFPPPZ/p1795200925169279). Didn't create any GitHub issues yet, or confirm which of the issues I ran into were already known.

Some of the annoyances:
- AI prompt didn't respect Enter, had to click the arrow to enter the prompt.
- View creation still gave me a broken suggested path.
- View creation process again failed to complete successfully and I needed to refresh to get rid of the popup with the spinner, even if the view (`SomethingElseView`) actually got created successfully.
    - I also didn't even get an error message about the failure to either client console or Eclipse console.
- All the changes were still added to the classes with zero indentation, and the order of operations was sometimes a bit confusing.
    - I only cleaned up the `AdminNav` changes manually.
- FormItem had a weird empty area above the content visually, but I'm not sure if that's intentional.
- It was a little bit confusing that it seemed to be impossible to create any new FormItems after creating one FormRow, but on the other hand I'm not sure what benefit I would have got from having FormItems over FormRows either.
- It was a bit more confusing to figure out how I could manage to trigger the popup again if I wanted it when I dragged something into the FormRow, instead of just squeezing the item there. But that was probably the whole point of the auto-responsiveness.
    - I did find the `Auto-Responsive` confusing at first -- I thought it might be for better responsiveness for different viewport sizes or something.
- Would have liked to have an option to convert that FormItem into a FormRow, but there wasn't even an option for AI prompt when I selected the FormItem from Outline.
    - I did figure out later that I could have just used the content paragraph's AI prompt, and I tested that it did work, but I reverted those changes because I found it useful to have the FormItem in there for testing.
- Several of my attempts to use the AI prompts failed, even if I never tried to chat with it and was always asking for code changes.
    - That's why I thought I would need to ask FormItem's own AI prompt, if it had had one. I figured from the other failures that I could only ask for changes that are about the selected element itself and strictly limited to configuring a limited subset of their own properties. I don't know if that's actually true or I just kept wanting things that weren't supported for other reasons.
    - Also, I needed two attempts to even manage to read the error popup because it faded out too fast.
    - I also got a different AI prompt-related failure from asking it to update the routing from a duplicated button's click listener to the new view, which the AI did attempt, but got wrong badly enough that the code no longer compiled. And after I fixed it manually and tried to return to edit mode, I got an error that claimed the file was corrupted and a restart was required.
    - I also found it kind of tricky to even find the options for the full view (needed to go to the Outline to select that element), although the thing I asked from the AI also wasn't supported, so it was a failed effort in the end anyway (see later for the point about `theme-editor.css`).
- The different 'sizes' of Text options within UI Library created no visual difference to the resulting paragraphs.
    - This does not include the `Text - inline` option, which had shorter contents and created a span instead.
- It was confusing that enabling experimental features added 7 new features by default but not the one that I actually wanted to test.
    - Unless those 7 features were already enabled by default, and if so, it's confusing why they can only be disabled if experimental features are on.
    - I didn't try to use any of those default features (unless I did so without intending to), so I don't have any other feedback about them.
- Optimistic Editing didn't display a new row between the existing rows where I placed it, but shoved it all the way to the bottom. By the time I figured out what happened to it I was just running out of the 5 seconds, and the view refreshed itself, and the row moved where it was supposed to be all along.
- I would have wanted a way to confirm immediately or undo my changes at times, in addition to the small delay, and the '2 pending edits' or something along those lines was confusing without any way to check what those edits actually were.
    - But at least it didn't keep increasing that number every time I dragged the new component elsewhere.
    - The 5 seconds felt a bit short when the component ended up hidden behind the UI Library or into a wrong place altogether like that abovementioned FormRow. Yet other times it felt too long, when I didn't have anything further that I even could have adjusted, like when I had deleted a paragraph. Of course I might have wanted to delete some other content as well, I expect the delay would not have felt annoying in that case.
    - Also mildly confusing when some operation like dragging the element stopped the timer but didn't immediately reset it visually, giving the impression that when I let go I'll have 2 seconds left to make any further adjustments.
    - It might also be useful to be able to configure the length of the delay. People have very different reaction times.
    - Might also like also an option to turn off the fixed delay altogether and only ever save the changes if I confirm it manually, but I do understand if that makes the feature too complicated.
- Now trying to drag an element (the span, in case it makes any difference) next to itself caused Optimistic Editing issues (`Optimistic preview has not completed successfully`) instead of the update never finishing (see my notes from previous hackathon). Dragging the span on itself gave me the insertion options popup, but I didn't have time to try what would happen with the different options.
- Theme editor triggered an immediate reload when I made the first change to some element, which was really annoying, especially since those changes were no longer visible after the reload.
    - I had the Optimistic Editing enabled already at that point, but there was never any delay. Didn't test it without Optimistic Editing.
    - A second edit to any feature on any element brought the previous changes back to visibility, and didn't trigger any more reloads.
    - I noticed later that I had an error about MIME type '' in my browser console, which blocked the `theme-editor.css` styles from getting applied, but I didn't have time to dig into that further. Just that apparently re-editing the same elements' styles seemed to get the changes past that block.
    - Took me a while to even figure out where the theme changes were written, and when I did, I thought I needed to add an import for the `theme-editor.css` manually (because I hadn't spotted that abovementioned error yet, and the AI prompt refused to even attempt to add the style import annotation, and I also hadn't spotted yet the import line that had already been added to `styles.css`). Of course, thanks to the error, the manually added import failed as well, and I ended up reverting it.
    - Also, if I still had the UI Library open when the reload happened, the Theme Editor ended up behind that.
- The hover borders of elements didn't keep up properly with scrolling. Sometimes they jumped to follow after a while, sometimes they stayed at the wrong position.
- Unrelated to my main testing, I also happened to notice that the Pie Chart from my previous hackathon changes was still causing issues if I tried to use Copilot within that view.


# hackathon-25-3
Now upgraded to 25.3. Experimenting with Vaadin Copilot in Eclipse again, works significantly better with the existing layouts than during the previous round. However, 3 Column Form is having significant issues (apparently related to the Pie Chart inside it overflowing), and now Outline is acting up and making a lot of the thing pretty much unusable.

More details and a screenshot in [Slack](https://vaadin.slack.com/archives/C0BUS9X6SQJ/p1788520729328779) and [GitHub](https://github.com/vaadin/copilot-internal/issues/9379)

Further experimenting with FormLayout also revealed an issue about updating never finishing if you accidentally try to drag a component next to itself. More details in [Slack](https://vaadin.slack.com/archives/C0BUS9X6SQJ/p1788526474243689) and [GitHub](https://github.com/vaadin/copilot-internal/issues/9380).

Did not come across any other issues.

# hackathon-25-2
Now upgraded to 25.2. Immediate issue with Jackson compatibility that was solved by upgrading `spring-boot-starter-parent` to 4.0.4. Tried at first with 4.1.0-RC1, but that gave me some `Cannot access Key[type=org.apache.maven.project.MavenProject, annotation=[none]] outside of a scoping block` error to my `pom.xml`.

Then I decided to try copilot, but had problems with enabling the hotswap agent. First I thought I had it already set up but turns out that was in VS Code, not in my Eclipse. Then I tried downloading three different versions of JetBrains Runtime and two different versions of Hotswap Agent and none of the combinations worked, I kept getting `Unrecognized VM option 'UpdateClasses'`. In the end I turned to my VS Code to figure out where it had saved the working configuration, and when I used that instead it worked. I don't know where I went wrong with the manual installations, beyond that as the very first thing I accidentally downloaded a non-SDK variant of JBR because I didn't spot that those were separate.

When the copilot was finally working I had problems with the copilot's UI elements blocking the top corners of my application, where the nav bar is. I tried adding a new button to the nav bar, but copilot created it within MapView instead and inserted it into the already initialized nav bar after the fact, and although I chose the option to add to the middle, it added it at the very end (where it was hidden by a copilot UI element). And when I asked AI to give the button a click listener that navigates to a new view, it created the new view as a static inner class of MapView and did some horrible hack for navigating back and forth, and when I actually tried the button, copilot started throwing internal errors about MapView being corrupted. So I extracted the new view to a new class myself, renamed it to NothingView, moved it to a correct package, fixed the contents, gave it a better route, added a new button to the nav bar manually, and reverted all changes to MenuView. After that the new view worked, but copilot couldn't recover from the changed route and kept claiming that the old one still exists and yet being unable to find the class when I tried to remove it. Mvn clean cleared that up.

Then I checked back with the recording what the _proper_ way to add a new view was, and tried that instead, but it kept autogenerating an invalid suggestion for the classname (`com\vaadin\example\sightseeing\views.MainView`), and it was rather awkward to change those `\` into dots, because the field didn't allow keyboard navigation (on Windows 11+Chrome). I tried the new icon selection, but as expected, it didn't really do anything in the end because the nav bar in this project is just a bunch of buttons rather than a Menu. And I don't know if that's related to it at all, but actually adding the view got copilot stuck and I had to refresh the page to get it to stop spinning the attempt to add the view. There was a warning triangle next to the spinner, but no popups and nothing in the server log, didn't think to check the browser console until after the fact so it's possible that there would have been something there. The new nav button I added myself because I didn't want to go through the same mess for a second time.

Then I tried to do some random dragging of UI elements into the new SomethingView, and within the view, and changing some 'hugs' to 'fills'. Around this stage I stopped fixing the mess that copilot left into my code for the sake of documenting it (has whitespace done something to offend copilot?). I still had to do several changes manually, because I kept getting compilation errors, because trying to move anything as the last element of any layout that has been initialized earlier seems to be broken and it doesn't move the initialization of the 'moved' element up. Adding as the first element or adding and expanding both work. I also kept needing to restart, because copilot kept complaining about corrupted view quite often even when it didn't manage to cause compilation errors, and it couldn't recover from either gracefully.

I meant to try the annotations too, but ran out of time.

# hackathon-25-1
Now upgraded to 25.1 via manually merging with a new starter project. Removed WebDriverManager and LineAwesome dependencies as unnecessary annoyances. Commented out `@OrderColumn` annotation from `Places.tags` because it caused issues (would have wanted a List instead of a Set, but triggered errors with the old database regardless), although that's not strictly speaking a Vaadin issue. Switched from using a theme to using `@StyleSheet` annotations, although I'm not convinced I did it correctly. Wasn't quite certain which of the new files should have been added to version control so may have excluded more of them than is ideal.

Updated tests to JUnit 6 and ran into the problem that a static import for `Assertions.assertEquals` refuses to work, even if it does work for other assertions and a non-static import works just fine. Also discovered that `vaadin-map-testbench` dependency isn't included by default, so had to add that separately to be able to keep using `MapElement` in the tests. Once the tests finally compiled they passed okay, but each test opened an additional browser window that never closed for some reason that I didn't have time to figure out.

Added some lines on the map with the new `LineStringFeature` (rectangles around the old office and the new office locations) but also ran out of time of adding any tests related to that.

# hackathon-24-7
Now upgraded to 24.7. Also updated osmapi-overpass from 2.0 to 3.0, added separate checks for user data and place data presence., added Grid tooltips with configured positions, added a horrible hack to change Tags View to support multi-selection, and added drag selection to Tags Grid.

Had some problems getting the project state to refresh properly after the initial version update, and the messed-up state kept interfering with the database creation, which was a bit of a blocker. But after multiple cleans, refreshes, vaadin-dances, and mvn installs it finally got the hint and after that my main problems were trying to remember how Flow works in general, and an old issue with line-awesome that I still didn't have time to dig into.

And in all fairness I didn't have time to do all that refreshing within the *previous* hackathon, so there may have been some leftovers from 24.2 still floating around some cache too...

The line-awesome issue is that after login it throws me to [some minified css page](http://localhost:8080/line-awesome/dist/line-awesome/css/line-awesome.min.css?continue) rather than [Map View](http://localhost:8080/). I'm probably doing something wrong with it, but I didn't even remember what it was before I started writing this and I don't have time to look into it any further.

# hackathon-24-6
Now upgraded to 24.6. This required updating Spring Boot version, which in turn caused a conflict with the `selenium.version` property (fixed by upgrading the version), and the H2 database was no longer compatible. Spent ages trying to get the database upgraded (with no success, even H2 Console won't run on my computer), until I had to give up and just re-generate it from scratch. Not ideal, but at least I could move forward.

Once I finally got the server up and running again I could log in, but for some reason that keeps now throwing me to http://localhost:8080/line-awesome/dist/line-awesome/css/line-awesome.min.css?continue instead of the actual application. But at least the application works again, if I navigate there manually.

Also added `org.parttio.line-awesome` dependency and removed the outdated feature flag from the previous round.

Ran out of time (again...) so didn't really get a chance to test the 24.6 features.

# hackathon-24-2
This is the 23.2 hackathon project that was earlier upgraded to 24.1 and now again to 24.2. Meant to try to setup web push notifications, but ran into environment issues and then ran out of time after all the installing and restarting.

## hackathon-24-1
Switched ID types from UUID to Long and added a button for adding a custom-styled marker.

The instructions below belong to the original version. 


## About the APP

### Repository Contents
This is a Vaadin 23.2 App that was downloaded from http://start.vaadin.com

It has 3 views, and there is no menu, so you need to type the URL for each page
  - /map - the map view - needs authentication
  - /places - master-detail for PLACE table - needs admin autentication
  - /tags - master-detail for TAG table - needs admin autentication

__NOTE__: for Hilla 1.2 users there is the [`main-hilla`](https://github.com/vaadin/hackathon-23-2/tree/main-hilla) branch instead.

### Data Generator
When the application starts, data generator creates:
  - Two users for login (user:user and admin:admin)
  - POIs for Turku (places and tags)
     - it uses a the [overpass api](https://wiki.openstreetmap.org/wiki/Overpass_API) querying a public [database](https://overpass-api.de)
     - if you want to fill the database with your city data change [`DataGenerator:CENTER`](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/java/com/vaadin/example/sightseeing/data/generator/DataGenerator.java#L34) and [`DataGenerator:RATIO`](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/java/com/vaadin/example/sightseeing/data/generator/DataGenerator.java#L35)

__NOTE__: Sometimes the query to the public service fails at first becausse a timeout (`OsmBadUserInputException: null`), re-run the app and it should work the second time because the server should have cached the results.

### Running the app

Just run `mvn` and open `https://localhost:8080` in your browser

__NOTE__: It requires JDK 17, but you can use 11 (see tips)

### Database
It uses spring JPA for managing an H2 database that persists in a local file, take a look to the [resources](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/resources/application.properties#L12) file.

H2 has a web console that you can use to check data, just open `http://localhost:8080/h2-console` and type `jdbc:h2:./h2-app` in the URL field, leaving User and Password fields empty.

### Committing your work
Create a branch with your name and push to this repo, eg:

```
git clone https://github.com/vaadin/hackathon-23-2.git
cd hackathon-23-2
mvn
git checkout -b my_named_branch
git commit -m 'My changes'
git push
```

### Showing your work

Write a summary of the main characteristics or your app, it could be in the README.md of your branch, or if you prefer a slide. Screenshots in the summary would be nice.

Optionally you can deploy a live demo in heroku or any accessible server.

You might want to demo your app to the audience at the end of the session.

## The Sightseeing APP

ACME is a Travel Agency that wants to offer their passengers an App that facilitates tourism in cities along the route.

Sightseeing-App works in a way that once users are authenticated, they can access the map and media files.

The Map will show the points of interest around their current position.

Clicking on each place, they can see all the information available for it, and be able to download audio guides, docs, etc.

The app should be usable in small devices.

### Users

No registration page, only ACME admins would be able to register new users

### Places and Tags

Admins can add/remove/modify places in the cities.

Places have a set of tags which are properties describing the POI, and they should be editable.

### Media

Admins can add to each place media files like audio, pictures, documents and videos

### Stats

Optionally there could be a statistic module showing the most visited places, etc.

## Hackathon Challenges

During the Hackathon, you might achieve any of the following challenges

1. Display a marker in the map showing the actual user position, it should be updated when user moves
2. Display markers of all the POIs around the user position
3. Display POI info when Clicking on It
4. Show a clickable list of actions available for each point (urls, media files, etc)
5. Visiting the city works in mobile devices (for admin pages, desktop is enough)
6. Display a button to switch to the admin views
7. Be able to edit Places and their tags as well as disable/enable them
8. Be able to updload media files per place
9. User cannot zoom/move out of the current city

Bonuses
1. App works offline
2. There is a stat module
3. Admin can edit/add POIs by right-clicking on the map
5. Anything you come with, eg. a chat

## Hackaton Goals

The main reason of the hackathon is not the app itself, but be able to play with the new features in Vaadin and give feedback about them.

We will evaluate the following aspects

- Design of the application
- Use cases covered
- Number of new features (v23.2) used in the app
- Number of issues found (if possible reported as tickets)

### New Features in 23.2

Please check the full feature list in the [roadmap page](https://github.com/orgs/vaadin/projects/9), though here you have a summary

- Map (not experimental)
- MenuBar right aligned theme
- MultiSelect Combobox
- TextField pattern
- Vite as default
- License checker offline
- TestBench + Karibu
- Hilla Multimodule
- Autocomplete intelliJ for WC
- Java API for Lumo icons

## Tips

- Compute current position of the user
    You can use this [approach](https://github.com/mstahv/maptesting/blob/main/src/main/java/com/example/application/MainView.java#L44) by [Matti](https://github.com/mstahv/)
- Menu for Admins
    You can combine here 'Java API for Lumo icons' + 'MenuBar right aligned theme'
- Place editor
    You can add the new feature multiselect Combobox for selecting tags    
- You can change the JDK to 11 instead of 17
`mvn versions:set-property -Dproperty=java.version -DnewVersion=11 -q`
- For adjusting POI info returned by overpass service, adjust the [query](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/java/com/vaadin/example/sightseeing/data/service/OverpassService.java#L27)
   - you can use this [frontend](https://overpass-turbo.eu/) for playing with querys
- To reset current database and re-run data generator, remove database file `rm -f ./h2-app.mv.db`
- Map documentation
  - [flow component](https://vaadin.com/docs/latest/components/map)
  - [web component](https://cdn-origin.vaadin.com/vaadin-web-components/23.2.0/index.html#/elements/vaadin-map)

