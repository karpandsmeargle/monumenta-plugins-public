" Remove whitespace from end of lines when saving
autocmd BufWritePre * :%s/\s\+$//e

" Use spaces in XML files
autocmd Filetype java set noexpandtab

" 4-width tabs by default
set expandtab
set shiftwidth=4
set tabstop=4
