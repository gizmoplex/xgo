package com.gizmoplex.bukkit.XGoPlugin;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.gizmoplex.bukkit.LocationReference;
import com.gizmoplex.bukkit.PluginDataAdapter;


public final class XGoPlugin extends JavaPlugin
{


  private HashMap<String, LocationReference> _publicLocations;
  private HashMap<String, HashMap<String, LocationReference>> _privateLocations;

  private PluginDataAdapter<HashMap<String, LocationReference>> _publicLocationsAdapter;
  private PluginDataAdapter<HashMap<String, HashMap<String, LocationReference>>> _privateLocationsAdapter;


  /***
   * Called when the the plugin is disabled.
   */
  @Override
  public void onDisable()
  {
    super.onDisable();

    // Save plugin data
    if (!savePluginData())
    {
      getLogger().severe("Unable to save plugin data.");
    }

    getLogger().info("XGo plugin disabled.");

  }


  /***
   * Called when the plugin is enabled.
   */
  @Override
  public void onEnable()
  {
    super.onEnable();

    PluginCommand cmd;

    // Init plugin data adapters
    initPluginDataAdapters();

    // Load plugin data from files
    if (!loadPluginData())
    {
      getLogger().severe("Unable to load plugin data.");
      setEnabled(false);
      return;
    }

    // go command
    cmd = getCommand("go");
    cmd.setExecutor(new GoCommandExecutor());
    cmd.setTabCompleter(new GoTabCompleter());

    // go-send command
    cmd = getCommand("go-send");
    cmd.setExecutor(new GoSendCommandExecutor());
    cmd.setTabCompleter(new GoSendTabCompleter());

    // go-add command
    cmd = getCommand("go-add");
    cmd.setExecutor(new GoAddCommandExecutor());

    // go-list command
    cmd = getCommand("go-list");
    cmd.setExecutor(new GoListCommandExecutor());

    // go-del command
    cmd = getCommand("go-del");
    cmd.setExecutor(new GoDelCommandExecutor());
    cmd.setTabCompleter(new GoDelTabCompleter());

    // Log message the plugin has been loaded
    getLogger().info("XGo plugin enabled.");

  }


  /***
   * Initializes plugin data adapters.
   */
  private void initPluginDataAdapters()
  {

    File folder;

    // Get the data folder and create it if necessary
    folder = getDataFolder();
    if (!folder.exists())
    {
      try
      {
        folder.mkdir();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }

    // Create adapters for plugin data
    _publicLocationsAdapter = new PluginDataAdapter<HashMap<String, LocationReference>>(getDataFolder() + File.separator + "publicLocations.bin");
    _privateLocationsAdapter = new PluginDataAdapter<HashMap<String, HashMap<String, LocationReference>>>(getDataFolder() + File.separator + "privateLocations.bin");

  }


  /***
   * Loads the plugin data from binary files. If the files do not exist, new
   * data objects are created.
   * 
   * @return
   */
  private boolean loadPluginData()
  {
    // Load public locations
    if (_publicLocationsAdapter.FileExists())
    {
      if (_publicLocationsAdapter.LoadObject())
      {
        _publicLocations = _publicLocationsAdapter.GetObject();
      }
      else
      {
        return (false);
      }
    }
    else
    {
      _publicLocations = new HashMap<String, LocationReference>();
      _publicLocationsAdapter.SetObject(_publicLocations);
    }

    // Load private locations
    if (_privateLocationsAdapter.FileExists())
    {
      if (_privateLocationsAdapter.LoadObject())
      {
        _privateLocations = _privateLocationsAdapter.GetObject();
      }
      else
      {
        return (false);
      }

    }
    else
    {
      _privateLocations = new HashMap<String, HashMap<String, LocationReference>>();
      _privateLocationsAdapter.SetObject(_privateLocations);
    }

    // Return successfully
    return (true);
  }


  /***
   * Saves plugin data to binary files.
   * 
   * @return If successful, true is returned. Otherwise, false is returned.
   */
  private boolean savePluginData()
  {
    boolean ret = true;

    // Save public locations
    if (!_publicLocationsAdapter.Save())
      ret = false;

    // Save private locations
    if (!_privateLocationsAdapter.Save())
      ret = false;

    // Return status
    return (ret);

  }


  /***
   * Returns the public locations hash map.
   * 
   * @return
   */
  public HashMap<String, LocationReference> getPublicLocations()
  {
    return (_publicLocations);
  }


  /***
   * Returns the private locations hash map for the specified player. If one
   * doesn't exist, a new one is created.
   * 
   * @param playerName
   *          - The name of the player.
   * @return
   */
  public HashMap<String, LocationReference> getPlayerLocations(String playerName)
  {
    HashMap<String, LocationReference> locations;

    // If player has no locations list yet, create it
    if (!_privateLocations.containsKey(playerName))
    {
      locations = new HashMap<String, LocationReference>();

      _privateLocations.put(playerName, locations);
    }
    else
    {
      locations = _privateLocations.get(playerName);
    }

    return (locations);
  }


  /***
   * Returns the private locations hash map.
   * 
   * @return
   */
  public HashMap<String, HashMap<String, LocationReference>> getPrivateLocations()
  {
    return (_privateLocations);
  }


  /***
   * Handles the go command
   * 
   * @author 
   * 
   */
  private class GoCommandExecutor implements CommandExecutor
  {


    /***
     * Handles the "go" command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
        String[] args)
    {
      Player player;
      Player destPlayer;
      String locationName;
      Location location;
      HashMap<String, LocationReference> locations;

      if (sender instanceof Player)
      {
        player = (Player) sender;

        // Must be exactly 1 argument
        if (args.length != 1)
        {
          player.sendMessage("Invalid number of arguments.");
          return (false);
        }

        // Get the location name
        locationName = args[0];

        // If location is another player (starts with "@")
        if (locationName.startsWith("@"))
        {
          // Player must have permission teleport to other players
          if (!player.hasPermission("XGoPlugin.go-player"))
          {
            player.sendMessage("You do not have permission to teleport to other players.");
            return (true);
          }

          // Get the destination player
          destPlayer = getServer().getPlayer(locationName.substring(1));

          // If player not found
          if (destPlayer == null)
          {
            player.sendMessage("Destination player not found.");
            return (true);
          }

          // Teleport the player to the destination player
          player.teleport(destPlayer);

        }
        else
        {

          // If public location (starts with "#")
          if (locationName.startsWith("#"))
          {
            // Player must have permission to use public locations
            if (!player.hasPermission("XGoPlugin.go-public"))
            {
              player.sendMessage("You do not have permission to use public locations.");
              return (true);
            }

            // Get public locations
            locations = getPublicLocations();
          }
          else
          {
            // Get player's locations
            locations = getPlayerLocations(player.getName());
          }

          // If location does not exist, error
          if (!locations.containsKey(locationName))
          {
            player.sendMessage("Location does not exist.");
            return (true);
          }

          // Retrieve the location
          location = locations.get(locationName).ceateLocation(XGoPlugin.this);

          // Teleport to the location
          player.teleport(location);

        }

      }
      else
      {
        sender.sendMessage("This command can only be executed by a player.");
        return (true);
      }

      return (true);
    }

  }


  /***
   * Handles the go-send command
   * 
   * @author 
   * 
   */
  private class GoSendCommandExecutor implements CommandExecutor
  {


    /***
     * Handles the "go-send" command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
        String[] args)
    {
      Player player;
      String srcPlayerName;
      Player srcPlayer;
      Player destPlayer;
      String locationName;
      Location location;
      HashMap<String, LocationReference> locations;

      if (sender instanceof Player)
      {
        player = (Player) sender;

        // Must be exactly 2 arguments
        if (args.length != 2)
        {
          player.sendMessage("Invalid number of arguments.");
          return (false);
        }

        // Get the source player name
        srcPlayerName = args[0];

        // Get the source player
        srcPlayer = getServer().getPlayer(srcPlayerName);

        // If source player not found, error
        if (srcPlayer == null)
        {
          player.sendMessage("Player \"" + srcPlayerName + "\" not found.");
          return (true);
        }

        // Get the location name
        locationName = args[1];

        // If location is another player (starts with "@")
        if (locationName.startsWith("@"))
        {
          // Player must have permission teleport to other players
          if (!player.hasPermission("XGoPlugin.go-player"))
          {
            player.sendMessage("You do not have permission to teleport to other players.");
            return (true);
          }

          // Get the destination player
          destPlayer = getServer().getPlayer(locationName.substring(1));

          // If player not found
          if (destPlayer == null)
          {
            player.sendMessage("Destination player not found.");
            return (true);
          }

          // Teleport the player to the destination player
          srcPlayer.teleport(destPlayer);

        }
        else
        {

          // If public location (starts with "#")
          if (locationName.startsWith("#"))
          {
            // Player must have permission to use public locations
            if (!player.hasPermission("XGoPlugin.go-public"))
            {
              player.sendMessage("You do not have permission to use public locations.");
              return (true);
            }

            // Get public locations
            locations = getPublicLocations();
          }
          else
          {
            // Get player's locations
            locations = getPlayerLocations(player.getName());
          }

          // If location does not exist, error
          if (!locations.containsKey(locationName))
          {
            player.sendMessage("Location does not exist.");
            return (true);
          }

          // Retrieve the location
          location = locations.get(locationName).ceateLocation(XGoPlugin.this);

          // Teleport the source player to the location
          srcPlayer.teleport(location);

        }
      }
      else
      {
        sender.sendMessage("This command can only be executed by a player.");
        return (true);
      }

      return (true);
    }

  }


  /***
   * Class to handle go-add command.
   * 
   * @author 
   * 
   */
  private class GoAddCommandExecutor implements CommandExecutor
  {


    /***
     * Handles the "go-add" command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
        String[] args)
    {
      Player player;
      String locationName;
      HashMap<String, LocationReference> locations;

      if (sender instanceof Player)
      {
        player = (Player) sender;

        // Must be exactly 1 argument
        if (args.length != 1)
        {
          player.sendMessage("Invalid number of arguments.");
          return (false);
        }

        // Get the location name
        locationName = args[0];

        // Location name limited to 20 characters
        if (locationName.length() > 20)
        {
          player.sendMessage("Location name is too long.  Max is 20.");
          return (true);
        }

        // Location name cannot start with "@"
        if (locationName.startsWith("@"))
        {
          player.sendMessage("Location name cannot start with \"@\".");
          return (true);
        }

        // If public location (starts with "#")
        if (locationName.startsWith("#"))
        {
          // Player must have permission to add public locations
          if (!player.hasPermission("XGoPlugin.go-add-public"))
          {
            player.sendMessage("You do not have permission to add public locations.");
            return (true);
          }

          // Get public locations
          locations = getPublicLocations();
        }
        else
        {
          locations = getPlayerLocations(player.getName());
        }

        // If location exists, error
        if (locations.containsKey(locationName))
        {
          player.sendMessage("Location already exists.");
          return (true);
        }

        // Save the location
        locations.put(locationName, new LocationReference(player.getLocation()));

        // Message
        player.sendMessage(locationName + " added.");

      }
      else
      {
        sender.sendMessage("This command can only be executed by a player.");
        return (true);
      }

      return (true);

    }

  }


  /***
   * Class to handle the go-del command
   * 
   * @author 
   * 
   */
  private class GoDelCommandExecutor implements CommandExecutor
  {


    /***
     * Handles the "go-del" command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
        String[] args)
    {
      Player player;
      String locationName;
      HashMap<String, LocationReference> locations;

      if (sender instanceof Player)
      {
        player = (Player) sender;

        // Must be exactly 1 argument
        if (args.length != 1)
        {
          player.sendMessage("Invalid number of arguments.");
          return (false);
        }

        // Get the location name
        locationName = args[0];

        // If public location (starts with "#")
        if (locationName.startsWith("#"))
        {
          // Player must have permission to delete public locations
          if (!player.hasPermission("XGoPlugin.go-del-public"))
          {
            player.sendMessage("You do not have permission to delete public locations.");
            return (true);
          }

          // Get public locations
          locations = getPublicLocations();
        }
        else
        {
          // Get player's locations
          locations = getPlayerLocations(player.getName());
        }

        // If location does not exist, error
        if (!locations.containsKey(locationName))
        {
          player.sendMessage("Location does not exist.");
          return (true);
        }

        // Delete the location
        locations.remove(locationName);

        // Message
        player.sendMessage(locationName + " deleted.");

      }
      else
      {
        sender.sendMessage("This command can only be executed by a player.");
        return (true);
      }

      return (true);
    }

  }


  /***
   * Class to handle the go-list command
   * 
   * @author 
   * 
   */
  private class GoListCommandExecutor implements CommandExecutor
  {


    /***
     * Handles the "go-list" command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
        String[] args)
    {
      Player player;
      Iterator<String> i;
      String locationName;
      String message;
      int maxLen = 60;

      if (sender instanceof Player)
      {
        player = (Player) sender;

        // Must not be any arguments
        if (args.length != 0)
        {
          player.sendMessage("Invalid number of arguments.");
          return (false);
        }

        // Start message
        player.sendMessage("go location list:");

        // Init message
        message = "";

        // Process the public locations
        if (player.hasPermission("XGoPlugin.go-list-public"))
        {
          i = getPublicLocations().keySet().iterator();
          while (i.hasNext())
          {
            // Get the location name
            locationName = i.next();

            // If this would exceed the line length
            if (message.length() + locationName.length() + 1 > maxLen)
            {
              player.sendMessage(message);
              message = "";
            }

            // Append the location
            if (message.length() > 0)
              message += " ";
            message += locationName;
          }
        }

        // Process the private locations
        i = getPlayerLocations(player.getName()).keySet().iterator();
        while (i.hasNext())
        {
          // Get the location name
          locationName = i.next();

          // If this would exceed the line length
          if (message.length() + locationName.length() + 1 > maxLen)
          {
            player.sendMessage(message);
            message = "";
          }

          // Append the location
          if (message.length() > 0)
            message += " ";
          message += locationName;
        }

        // Send the last line of locations
        if (message.length() > 0)
          player.sendMessage(message);
        else
          player.sendMessage("There are no locations.");

      }
      else
      {
        sender.sendMessage("This command can only be executed by a player.");
        return (true);
      }

      return (true);
    }

  }


  /***
   * Class to handle tab completion for go command
   * 
   * @author 
   * 
   */
  private class GoTabCompleter implements TabCompleter
  {


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
        String alias, String[] args)
    {
      List<String> locationNames = new ArrayList<String>();
      Player player;
      String playerName;
      Player[] onlinePlayers;
      Player onlinePlayer;
      String onlinePlayerName;
      Iterator<String> i;
      String locationName;

      // If not the first argument, return empty list
      if (args.length != 1)
        return (locationNames);

      if (sender instanceof Player)
      {
        player = (Player) sender;
        playerName = player.getName();

        // Process the public locations
        if (player.hasPermission("XGoPlugin.go-public"))
        {
          i = getPublicLocations().keySet().iterator();
          while (i.hasNext())
          {
            // Get the location name
            locationName = i.next();

            // Add the location name to the list
            locationNames.add(locationName);
          }
        }

        // Process the private locations
        i = getPlayerLocations(playerName).keySet().iterator();
        while (i.hasNext())
        {
          // Get the location name
          locationName = i.next();

          // Add the location name to the list
          locationNames.add(locationName);
        }

        // Process other online players
        if (player.hasPermission("XGoPlugin.go-player"))
        {
          onlinePlayers = getServer().getOnlinePlayers();

          for (int j = 0; j < onlinePlayers.length; j++)
          {
            onlinePlayer = onlinePlayers[j];
            onlinePlayerName = onlinePlayer.getName();

            // If not current player
            if (!onlinePlayerName.equals(playerName))
            {
              locationNames.add("@" + onlinePlayerName);
            }
          }
        }

      }

      return (locationNames);
    }

  }


  /***
   * Class to handle tab completion for go-send command
   * 
   * @author 
   * 
   */
  private class GoSendTabCompleter implements TabCompleter
  {


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
        String alias, String[] args)
    {
      List<String> names = new ArrayList<String>();
      Player player;
      String playerName;
      Player[] onlinePlayers;
      Player onlinePlayer;
      String onlinePlayerName;
      Iterator<String> i;
      String locationName;


      if (sender instanceof Player)
      {
        player = (Player) sender;
        playerName = player.getName();

        // If processing the first argument
        if (args.length == 1)
        {
          onlinePlayers = getServer().getOnlinePlayers();

          for (int j = 0; j < onlinePlayers.length; j++)
          {
            onlinePlayer = onlinePlayers[j];
            onlinePlayerName = onlinePlayer.getName();

            // If not current player
            if (!onlinePlayerName.equals(playerName))
            {
              names.add(onlinePlayerName);
            }
          }
        }
        // Else, if processing the second argument
        else if (args.length == 2)
        {
          // Process the public locations
          if (player.hasPermission("XGoPlugin.go-public"))
          {
            i = getPublicLocations().keySet().iterator();
            while (i.hasNext())
            {
              // Get the location name
              locationName = i.next();

              // Add the location name to the list
              names.add(locationName);
            }
          }

          // Process the private locations
          i = getPlayerLocations(playerName).keySet().iterator();
          while (i.hasNext())
          {
            // Get the location name
            locationName = i.next();

            // Add the location name to the list
            names.add(locationName);
          }

          // Process other online players
          if (player.hasPermission("XGoPlugin.go-player"))
          {
            onlinePlayers = getServer().getOnlinePlayers();

            for (int j = 0; j < onlinePlayers.length; j++)
            {
              onlinePlayer = onlinePlayers[j];
              onlinePlayerName = onlinePlayer.getName();

              // If not current player
              if (!onlinePlayerName.equals(playerName))
              {
                names.add("@" + onlinePlayerName);
              }
            }
          }
        }
      }

      return (names);
    }

  }


  /***
   * Class to handle tab completion for go-del command
   * 
   * @author 
   * 
   */
  private class GoDelTabCompleter implements TabCompleter
  {


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
        String alias, String[] args)
    {
      List<String> locationNames = new ArrayList<String>();
      Player player;
      String playerName;
      Iterator<String> i;
      String locationName;

      // If not the first argument, return empty list
      if (args.length != 1)
        return (locationNames);

      if (sender instanceof Player)
      {
        player = (Player) sender;
        playerName = player.getName();

        // Process the public locations
        if (player.hasPermission("XGoPlugin.go-del-public"))
        {
          i = getPublicLocations().keySet().iterator();
          while (i.hasNext())
          {
            // Get the location name
            locationName = i.next();

            // Add the location name to the list
            locationNames.add(locationName);
          }
        }

        // Process the private locations
        i = getPlayerLocations(playerName).keySet().iterator();
        while (i.hasNext())
        {
          // Get the location name
          locationName = i.next();

          // Add the location name to the list
          locationNames.add(locationName);
        }

      }

      return (locationNames);
    }

  }

}