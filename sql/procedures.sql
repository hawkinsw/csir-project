DELIMITER ^
use ir^
drop procedure if exists parent_list^
create procedure `parent_list`(in root integer)
begin
	set @plist := "";
	set @c := root;
	set @continue := true;
	while (@continue = true) do
		select count(*), parent_id into @rootexists, @newroot 
			from parents where source_id = @c 
			limit 1;
		if (@rootexists != 0 and @newroot != 0) then
			set @plist = concat(@newroot,",",@plist);
			set @c := @newroot;
		else
			set @continue = false;
		end if;
	end while;
	select @plist;
end^
